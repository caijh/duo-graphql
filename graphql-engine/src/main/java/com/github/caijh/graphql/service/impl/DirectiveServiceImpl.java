package com.github.caijh.graphql.service.impl;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.caijh.graphql.core.GraphqlConsts;
import com.github.caijh.graphql.core.exception.GraphqlInvocationException;
import com.github.caijh.graphql.directive.BaseGraphqlDirectiveFactory;
import com.github.caijh.graphql.service.DirectiveService;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import graphql.language.Argument;
import graphql.language.BooleanValue;
import graphql.language.Directive;
import graphql.language.FloatValue;
import graphql.language.IntValue;
import graphql.language.NullValue;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLDirective;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * @author xuwenzhen
 * @date 2019/7/15
 */
@Service
public class DirectiveServiceImpl implements DirectiveService {

    private static final Logger logger = LoggerFactory.getLogger(DirectiveServiceImpl.class);

    @Autowired
    private List<BaseGraphqlDirectiveFactory> graphqlDirectiveList;

    private Set<GraphQLDirective> directiveSet;

    private Map<String, BaseGraphqlDirectiveFactory> graphqlDirectiveMap;

    /**
     * 获取所有已注册的指令
     *
     * @return 指令
     */
    @Override
    public Set<GraphQLDirective> getDirectiveSet() {
        this.init();
        return this.directiveSet;
    }

    @Override
    public void processDirective(DataFetchingEnvironment environment, Object data, Map<String, List<Directive>> fieldDirectiveMap) {
        if (data == null) {
            return;
        }

        fieldDirectiveMap.entrySet().forEach(fieldEntry -> {
            String path = fieldEntry.getKey();
            List<Directive> directives = fieldEntry.getValue();
            directives.forEach(directive -> this.processFieldDirective(environment, path, data, directive));
        });
    }

    private void processFieldDirective(DataFetchingEnvironment environment, String path, Object data, Directive directive) {
        String directiveName = directive.getName();
        this.init();
        BaseGraphqlDirectiveFactory graphqlDirective = this.graphqlDirectiveMap.get(directiveName);
        if (graphqlDirective == null) {
            logger.warn("{}指令@{}，暂未实现！", path, directiveName);
            return;
        }

        //获取参数
        Map<String, Object> argMap = Maps.newHashMap();
        List<Argument> args = directive.getArguments();
        if (args != null) {
            args.forEach(argument -> argMap.put(argument.getName(), this.getArgumentValue(argument.getValue())));
        }
        this.getPathValue(environment, path, data, graphqlDirective, argMap);
    }

    private Object getPathValue(DataFetchingEnvironment environment, String path, Object data, BaseGraphqlDirectiveFactory directive, Map<String, Object> argMap) {
        if (data == null) {
            return null;
        }

        if (data instanceof List) {
            List listData = (List) data;
            for (int i = 0; i < listData.size(); i++) {
                Object item = listData.get(i);
                Object pathValue;
                pathValue = this.getPathValue(environment, path, item, directive, argMap);
                listData.set(i, pathValue);
            }
            return listData;
        } else if (data instanceof Map) {
            int index = path.indexOf(GraphqlConsts.CHAR_DOT);
            String currentPath;
            Map mapData = (Map) data;
            if (index == -1) {
                Object pathValue = mapData.get(path);
                pathValue = directive.process(environment, pathValue, argMap);
                mapData.put(path, pathValue);
            } else {
                currentPath = path.substring(0, index);
                String nextPath = path.substring(index + 1);
                Object currentData = mapData.get(currentPath);
                if (currentData != null) {
                    this.getPathValue(environment, nextPath, currentData, directive, argMap);
                }
            }
            return mapData;
        } else {
            throw new GraphqlInvocationException("无法解决路径:" + path + "的值：" + data);
        }
    }

    private Object getArgumentValue(Value value) {
        if (value instanceof IntValue) {
            return ((IntValue) value).getValue().intValue();
        } else if (value instanceof BooleanValue) {
            return ((BooleanValue) value).isValue();
        } else if (value instanceof FloatValue) {
            return ((FloatValue) value).getValue().floatValue();
        } else if (value instanceof StringValue) {
            return ((StringValue) value).getValue();
        } else if (value instanceof NullValue) {
            return null;
        } else {
            throw new GraphqlInvocationException("暂不支持指令参数类型，value=" + value.toString());
        }
    }

    private synchronized void init() {
        if (this.graphqlDirectiveMap != null) {
            return;
        }
        this.graphqlDirectiveMap = Maps.newHashMap();
        this.directiveSet = Sets.newHashSet();
        if (CollectionUtils.isEmpty(this.graphqlDirectiveList)) {
            return;
        }
        this.graphqlDirectiveList.forEach(directiveFactory -> {
            GraphQLDirective directive = directiveFactory.getGraphQLDirective();
            this.graphqlDirectiveMap.put(directive.getName(), directiveFactory);
            this.directiveSet.add(directive);
        });
    }

}
