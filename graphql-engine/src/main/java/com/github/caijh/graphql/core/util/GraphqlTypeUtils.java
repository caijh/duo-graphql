package com.github.caijh.graphql.core.util;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.github.caijh.graphql.core.GraphqlConsts;
import com.github.caijh.graphql.core.exception.GraphqlBuildException;
import com.github.caijh.graphql.core.exception.GraphqlInvocationException;
import com.github.caijh.graphql.pipeline.RegistryState;
import com.github.caijh.graphql.provider.dto.provider.Entity;
import com.github.caijh.graphql.provider.dto.provider.EntityRef;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import graphql.Scalars;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLEnumValueDefinition;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import static graphql.Scalars.GraphQLBigDecimal;
import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLByte;
import static graphql.Scalars.GraphQLFloat;
import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLLong;
import static graphql.Scalars.GraphQLShort;

/**
 * @author xuwenzhen
 * @date 2019/4/29
 */
public class GraphqlTypeUtils {

    private static final Logger logger = LoggerFactory.getLogger(GraphqlTypeUtils.class);

    public static final String PATH_VARIABLE = "@PathVariable";
    public static final String REQUEST_BODY = "@RequestBody";

    private static final Map<String, GraphQLType> GRAPHQL_TYPE_MAP = Maps.newHashMap();

    static {
        GRAPHQL_TYPE_MAP.put("String", Scalars.GraphQLString);
        GRAPHQL_TYPE_MAP.put(String.class.getName(), Scalars.GraphQLString);

        GRAPHQL_TYPE_MAP.put("boolean", GraphQLBoolean);
        GRAPHQL_TYPE_MAP.put("Boolean", GraphQLBoolean);
        GRAPHQL_TYPE_MAP.put(Boolean.class.getName(), GraphQLBoolean);

        GRAPHQL_TYPE_MAP.put("int", GraphQLInt);
        GRAPHQL_TYPE_MAP.put("Integer", GraphQLInt);
        GRAPHQL_TYPE_MAP.put(Integer.class.getName(), GraphQLInt);

        GRAPHQL_TYPE_MAP.put("flat", GraphQLFloat);
        GRAPHQL_TYPE_MAP.put("Float", GraphQLFloat);
        GRAPHQL_TYPE_MAP.put(Float.class.getName(), GraphQLFloat);

        GRAPHQL_TYPE_MAP.put("double", GraphQLFloat);
        GRAPHQL_TYPE_MAP.put("Double", GraphQLFloat);
        GRAPHQL_TYPE_MAP.put(Double.class.getName(), GraphQLFloat);

        GRAPHQL_TYPE_MAP.put("long", GraphQLLong);
        GRAPHQL_TYPE_MAP.put("Long", GraphQLLong);
        GRAPHQL_TYPE_MAP.put(Long.class.getName(), GraphQLLong);

        GRAPHQL_TYPE_MAP.put("short", GraphQLShort);
        GRAPHQL_TYPE_MAP.put("Short", GraphQLShort);
        GRAPHQL_TYPE_MAP.put(Short.class.getName(), GraphQLShort);

        GRAPHQL_TYPE_MAP.put("byte", GraphQLByte);
        GRAPHQL_TYPE_MAP.put("Byte", GraphQLByte);
        GRAPHQL_TYPE_MAP.put(Byte.class.getName(), GraphQLByte);

        GRAPHQL_TYPE_MAP.put("float", GraphQLFloat);
        GRAPHQL_TYPE_MAP.put("Float", GraphQLFloat);
        GRAPHQL_TYPE_MAP.put(Float.class.getName(), GraphQLFloat);

        GRAPHQL_TYPE_MAP.put("BigDecimal", GraphQLBigDecimal);
        GRAPHQL_TYPE_MAP.put(BigDecimal.class.getName(), GraphQLBigDecimal);

        GRAPHQL_TYPE_MAP.put("Date", GraphQLLong);
        GRAPHQL_TYPE_MAP.put(Date.class.getName(), GraphQLLong);
    }

    public static GraphQLType getBaseGraphQLType(String typeName) {
        return GRAPHQL_TYPE_MAP.get(typeName);
    }

    private GraphqlTypeUtils() {
    }

    public static String getFieldTypeName(GraphQLType fieldType) {
        if (fieldType instanceof GraphQLList) {
            return getFieldTypeName(((GraphQLList) fieldType).getWrappedType());
        }
        return fieldType.getName();
    }

    public static GraphQLOutputType getFieldGraphQLOutputType(GraphQLOutputType type, String fieldName) {
        if (type instanceof GraphQLObjectType) {
            GraphQLFieldDefinition fieldDefinition = ((GraphQLObjectType) type).getFieldDefinition(fieldName);
            if (fieldDefinition == null) {
                throw new GraphqlInvocationException("GraphQLObjectType???" + type.getName() + "????????????????????????" + fieldName);
            }
            return fieldDefinition.getType();
        } else if (type instanceof GraphQLList) {
            return getFieldGraphQLOutputType((GraphQLOutputType) ((GraphQLList) type).getWrappedType(), fieldName);
        }
        throw new GraphqlInvocationException(type.getName() + "?????????????????????" + fieldName);
    }

    public static GraphQLType getGraphqlInputType(RegistryState registryState, String moduleName, String entityName) {
        GraphQLType baseGraphQLType = GraphqlTypeUtils.getBaseGraphQLType(entityName);
        if (baseGraphQLType != null) {
            return baseGraphQLType;
        }
        Entity entity = registryState.getEntity(entityName);
        if (entity == null) {
            logger.warn("???????????????????????????{}", entityName);
            return null;
        }

        Boolean map = entity.getMap();
        if (map != null && map) {
            // Map
            throw new GraphqlBuildException("?????????Map????????????????????????");
        }
        Boolean collection = entity.getCollection();
        if (collection != null && collection) {
            // ??????
            if (CollectionUtils.isEmpty(entity.getParameteredEntityRefs())) {
                throw new GraphqlBuildException("?????????????????????:" + entityName);
            }
            EntityRef parameteredEntityRef = entity.getParameteredEntityRefs().get(0);
            GraphQLType paramGraphQLType = getGraphqlInputType(registryState, moduleName, parameteredEntityRef.getEntityName());
            if (paramGraphQLType == null) {
                throw new GraphqlBuildException("????????????????????????:" + entityName);
            }
            return addAndGetGraphQLListType(paramGraphQLType);
        }

        boolean isEnum = entity.getEnumerate() != null && entity.getEnumerate();
        if (isEnum) {
            //???????????????
            return addAndGetGraphQLEnumType(registryState, moduleName, entity);
        }

        GraphQLType graphQLType = registryState.getGraphQLType(entityName);
        if (graphQLType != null) {
            return graphQLType;
        }

        //Pojo
        if (CollectionUtils.isEmpty(entity.getFields())) {
            throw new GraphqlBuildException("??????????????????: " + entityName);
        }

        return addAndGetInputGraphQLType(registryState, moduleName, entity);
    }

    /**
     * create enum type
     */
    public static GraphQLEnumType addAndGetGraphQLEnumType(RegistryState registryState, String moduleName, Entity entity) {
        String name = GraphqlTypeUtils.getModuleTypeName(entity, moduleName);
        GraphQLType type = registryState.getGraphQLType(name);

        if (type != null) {
            return (GraphQLEnumType) type;
        }
        if (CollectionUtils.isEmpty(entity.getFields())) {
            throw new GraphqlBuildException("??????:" + entity.getName() + ",???????????????");
        }

        GraphQLEnumType.Builder enumTypeBuilder = GraphQLEnumType.newEnum().name(name);
        if (!StringUtils.isEmpty(entity.getComment())) {
            enumTypeBuilder.description(entity.getComment());
        }

        entity.getFields().forEach(field -> {
            GraphQLEnumValueDefinition.Builder enumValueBuild =
                    GraphQLEnumValueDefinition.newEnumValueDefinition().name(field.getName());
            if (!StringUtils.isEmpty(field.getComment())) {
                enumValueBuild.description(field.getComment());
            }
            enumValueBuild.value(field.getName());
            enumTypeBuilder.value(enumValueBuild.build());
        });
        GraphQLEnumType graphQLEnumType = enumTypeBuilder.build();
        registryState.addGraphQLType(name, graphQLEnumType);
        return graphQLEnumType;
    }

    private static GraphQLType addAndGetInputGraphQLType(RegistryState registryState, String moduleName, Entity entity) {
        if (CollectionUtils.isEmpty(entity.getFields())) {
            throw new GraphqlBuildException("Pojo???" + entity.getName() + "???????????????");
        }

        String name = getModuleTypeName(entity, moduleName);
        List<GraphQLInputObjectField> graphQLFieldDefinitions = Lists.newArrayList();
        entity.getFields().forEach(field -> {
            GraphQLInputType fieldType = (GraphQLInputType) getGraphqlInputType(registryState, moduleName, field.getEntityName());
            if (fieldType == null) {
                if (entity.getEnumerate() != null && entity.getEnumerate()) {
                    //???????????????

                }
                return;
            }
            String fieldName = field.getName();

            GraphQLInputObjectField.Builder fieldDefinition = GraphQLInputObjectField.newInputObjectField()
                                                                                     .name(fieldName)
                                                                                     .type(fieldType);
            if (!StringUtils.isEmpty(field.getComment())) {
                fieldDefinition.description(field.getComment());
            }
            graphQLFieldDefinitions.add(fieldDefinition.build());
        });

        GraphQLInputObjectType.Builder objectTypeBuilder = GraphQLInputObjectType.newInputObject()
                                                                                 .name(name)
                                                                                 .fields(graphQLFieldDefinitions);

        if (!StringUtils.isEmpty(entity.getComment())) {
            objectTypeBuilder.description(entity.getComment());
        }

        GraphQLType graphQLType = objectTypeBuilder.build();
        registryState.addGraphQLType(entity.getName(), graphQLType);
        return graphQLType;
    }

    private static GraphQLList addAndGetGraphQLListType(GraphQLType graphqlType) {
        return GraphQLList.list(graphqlType);
    }

    public static String getModuleTypeName(Entity entity, String moduleName) {
        return getModuleTypeName(moduleName, getSimpleName(entity.getName()));
    }

    public static String getModuleTypeName(String groupName, String simpleEntityName) {
        return groupName + GraphqlConsts.STR_XHX + simpleEntityName;
    }

    private static String getSimpleName(String name) {
        if (!name.contains(GraphqlConsts.STR_LT)) {
            int index = name.lastIndexOf(GraphqlConsts.CHAR_DOT);
            String simpleName = name.substring(index + 1);
            if (simpleName.contains(GraphqlConsts.STR_GT)) {
                simpleName = simpleName.replaceAll(GraphqlConsts.STR_GT, GraphqlConsts.STR_EMPTY);
            }
            return simpleName;
        }
        String[] names = name.split(GraphqlConsts.STR_LT);

        StringBuilder sb = new StringBuilder();
        for (String name1 : names) {
            if (sb.length() > 0) {
                sb.append("_");
            }
            sb.append(getSimpleName(name1));
        }
        return sb.toString();
    }

}
