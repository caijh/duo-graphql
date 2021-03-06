package com.github.caijh.graphql.provider;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.github.caijh.graphql.provider.annotation.GraphqlModule;
import com.github.caijh.graphql.provider.annotation.IdProvider;
import com.github.caijh.graphql.provider.annotation.IdsProvider;
import com.github.caijh.graphql.provider.annotation.SchemaProvider;
import com.github.caijh.graphql.provider.dto.ProviderModelInfo;
import com.github.caijh.graphql.provider.dto.TpDocGraphqlProviderServiceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * base graphql registry
 *
 * @author xuwenzhen
 * @since 2019/9/8
 */
public abstract class BaseProviderRegistry {

    private static final Logger logger = LoggerFactory.getLogger(BaseProviderRegistry.class);
    protected static final String CHARSET_NAME = "utf-8";
    private static final String GIT_PROPERTIES = "git.properties";
    private static final Pattern COMMIT_ID_PATTERN = Pattern.compile(".*\"git.commit.id\"\\s?:\\s?\"(.*?)\".*", Pattern.MULTILINE);
    private static final String STR_DOT = ".";

    /**
     * 当前服务提供的服务领域名称（为了方便，尽量简写，比如Xf / Esf / Agent...）
     */
    @Value("${graphql.schema.module:}")
    private String schemaModuleName;

    /**
     * 指定GraphQL Schema的名称
     */
    @Value("${graphql.schema.name:}")
    private String schemaName;

    /**
     * 当前服务的调用地址，用于Graphql引擎DataProvider调用本服务接口
     */
    @Value("${application.server:}")
    private String server;

    /**
     * 当前服务名称，需要与Mesh网格一致
     */
    @Value("${spring.application.name:}")
    private String applicationName;

    @Autowired
    protected ApplicationContext applicationContext;

    /**
     * 基础验证
     */
    protected void validate() {
        if (this.applicationName == null) {
            throw new GraphqlProviderException("未配置spring.application.name!");
        }

        String commitId = this.getCommitId();
        if (commitId == null || commitId.length() == 0) {
            throw new GraphqlProviderException("读取Git信息文件失败！git.commit.id为空！");
        }

        if (StringUtils.isEmpty(this.schemaModuleName)) {
            throw new GraphqlProviderException("作为GraphQL数据供应端，graphql.schema.module不能为空！");
        }
    }

    protected TpDocGraphqlProviderServiceInfo getTpDocGraphqlProviderServiceInfo() {
        TpDocGraphqlProviderServiceInfo provider = new TpDocGraphqlProviderServiceInfo();
        provider.setAppId(this.applicationName);
        provider.setVcsId(this.getCommitId());
        provider.setModuleName(this.schemaModuleName);
        provider.setServer(this.server);
        provider.setSchemaName(this.schemaName);

        List<ProviderModelInfo> models = new ArrayList<>();
        Map<String, Object> beans = this.applicationContext.getBeansWithAnnotation(SchemaProvider.class);
        if (!beans.isEmpty()) {
            for (Map.Entry<String, Object> entry : beans.entrySet()) {
                ProviderModelInfo modelInfo = this.getProviderModelInfo(entry);
                if (modelInfo != null) {
                    models.add(modelInfo);
                }
            }
            if (!CollectionUtils.isEmpty(models)) {
                provider.setModels(models);
            }
        }

        //获取@GraphqlModule注解
        this.setGraphqlModuleMap(provider);

        return provider;
    }

    private void setGraphqlModuleMap(TpDocGraphqlProviderServiceInfo provider) {
        Map<String, Object> graphqlModuleMap = this.applicationContext.getBeansWithAnnotation(GraphqlModule.class);
        if (!graphqlModuleMap.isEmpty()) {
            Map<String, String> moduleMap = new HashMap<>(8);
            graphqlModuleMap.entrySet().forEach(entry -> {
                Object controller = entry.getValue();
                Class<?> controllerClass = controller.getClass();
                String moduleName = controllerClass.getAnnotation(GraphqlModule.class).value();
                String controllerName = controllerClass.getName();
                String existsController = moduleMap.get(moduleName);
                if (existsController == null) {
                    moduleMap.put(moduleName, controllerName);
                } else {
                    moduleMap.put(moduleName, existsController + "," + controllerName);
                }
            });
            provider.setModuleMap(moduleMap);
        }
    }

    private ProviderModelInfo getProviderModelInfo(Map.Entry<String, Object> entry) {
        Object providerObj = entry.getValue();
        Class<?> controllerClass = providerObj.getClass();
        SchemaProvider schemaProvider = controllerClass.getAnnotation(SchemaProvider.class);
        ProviderModelInfo modelInfo = new ProviderModelInfo();
        modelInfo.setModelName(schemaProvider.clazz().getSimpleName());
        Set<String> refIds = new HashSet<>();
        Collections.addAll(refIds, schemaProvider.ids());
        modelInfo.setRefIds(refIds);

        Method[] methods = controllerClass.getDeclaredMethods();
        if (methods.length == 0) {
            return null;
        }
        String className = controllerClass.getName();

        boolean hasIdApi = false;
        boolean hasIdsApi = false;
        for (Method method : methods) {
            if (method.isAnnotationPresent(IdProvider.class)) {
                modelInfo.setIdProvider(className + STR_DOT + method.getName());
                hasIdApi = true;
            } else if (method.isAnnotationPresent(IdsProvider.class)) {
                modelInfo.setIdsProvider(className + STR_DOT + method.getName());
                hasIdsApi = true;
            }
        }

        if (!hasIdApi && !hasIdsApi) {
            logger.warn("{}没有指定id查询接口！", className);
            return null;
        }
        return modelInfo;
    }

    /**
     * 构建时生成的git.properties文件中读取commitId
     *
     * @return commitId
     */
    private String getCommitId() {
        String gitInfo = this.readResourceString(GIT_PROPERTIES);
        if (gitInfo == null || gitInfo.length() == 0) {
            throw new GraphqlProviderException("读取Git信息文件失败！");
        }

        Matcher matcher = COMMIT_ID_PATTERN.matcher(gitInfo);
        if (!matcher.find()) {
            throw new GraphqlProviderException("读取Git信息文件失败！未找到git.commit.id");
        }

        return matcher.group(1);
    }

    /**
     * 通过资源ID，获取资源文件内容
     *
     * @param resourceName 资源ID
     * @return 资源文本
     */
    protected String readResourceString(String resourceName) {
        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream(resourceName)) {
            if (is != null) {
                return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)).lines()
                                                                                            .collect(Collectors.joining(System.lineSeparator()));
            }
        } catch (Exception e) {
            throw new GraphqlProviderException("读取资源" + resourceName + "文件失败！", e);
        }
        return null;
    }

    /**
     * 注册服务
     *
     * @param provider 服务信息
     */
    protected abstract void registerProvider(TpDocGraphqlProviderServiceInfo provider);

}
