package com.github.caijh.graphql.service.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.PostConstruct;

import com.github.caijh.graphql.core.ExecutionMonitor;
import com.github.caijh.graphql.core.GraphqlConsts;
import com.github.caijh.graphql.core.GraphqlModuleContext;
import com.github.caijh.graphql.core.GraphqlProviderObserver;
import com.github.caijh.graphql.core.config.GraphqlInvocationConfigure;
import com.github.caijh.graphql.core.util.GraphqlContextUtils;
import com.github.caijh.graphql.fetcher.SubscriptionDataFetcherProxy;
import com.github.caijh.graphql.pipeline.Pipeline;
import com.github.caijh.graphql.pipeline.PipelineManager;
import com.github.caijh.graphql.pipeline.RegistryState;
import com.github.caijh.graphql.provider.InnerProvider;
import com.github.caijh.graphql.provider.dto.TpDocGraphqlProviderServiceInfo;
import com.github.caijh.graphql.register.server.GraphqlEngineService;
import com.github.caijh.graphql.service.DirectiveService;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import graphql.GraphQL;
import graphql.analysis.MaxQueryDepthInstrumentation;
import graphql.execution.SubscriptionExecutionStrategy;
import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.preparsed.PreparsedDocumentProvider;
import graphql.schema.DataFetcher;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLDirective;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLTypeReference;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import static graphql.Scalars.GraphQLString;

/**
 * 基于Duo-Doc的Graphql Provider注册实现
 *
 * @author xuwenzhen
 * @date 2019/4/9
 */
@Service
public class DuoDocGraphqlEngineServiceImpl implements GraphqlEngineService<TpDocGraphqlProviderServiceInfo> {

    private static final Logger logger = LoggerFactory.getLogger(DuoDocGraphqlEngineServiceImpl.class);

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private GraphqlInvocationConfigure graphqlInvocationConfigure;

    @Autowired(required = false)
    private List<InnerProvider> innerProviders;

    @Autowired
    private PipelineManager pipelineManager;

    @Autowired
    private DirectiveService directiveService;

    @Autowired(required = false)
    private ExecutionMonitor executionMonitor;

    @Value("${graphql.query.max-depth:14}")
    private int graphqlQueryMaxDepth;

    @Autowired
    private List<Pipeline> pipelines;

    @Autowired
    private GraphqlProviderObserver graphqlProviderObserver;

    @Autowired
    private PreparsedDocumentProvider preparsedDocumentProvider;

    private ObservableEmitter<List<TpDocGraphqlProviderServiceInfo>> providerEmitter;

    /**
     * Spring框架加载时完成一些初始化动作
     */
    @PostConstruct
    public void initGraphqlContext() {
        GraphqlContextUtils.setApplicationContext(this.applicationContext);
        //重新排序，小的在前
        Collections.sort(this.pipelines, ((Pipeline o1, Pipeline o2) -> o1.order() - o2.order()));

        if (!CollectionUtils.isEmpty(this.innerProviders)) {
            GraphqlContextUtils.setInnerProviders(this.innerProviders);
        }
        Observable<List<TpDocGraphqlProviderServiceInfo>> providerObservable = Observable
                .create((ObservableEmitter<List<TpDocGraphqlProviderServiceInfo>> emitter) -> {
                    logger.info("init Observable and create a emitter...");
                    this.providerEmitter = emitter;
                });
        providerObservable.subscribe(this.graphqlProviderObserver);
    }

    @Override
    public void emitProviderList(List<TpDocGraphqlProviderServiceInfo> providerList) {
        this.providerEmitter.onNext(providerList);
    }

    @Override
    public void registry(String schemaName, List<TpDocGraphqlProviderServiceInfo> providerServiceDataList) {
        //注册
        RegistryState state = null;
        if (this.executionMonitor != null) {
            this.executionMonitor.beforeSchemaBuild(schemaName, providerServiceDataList);
        }
        try {
            state = this.pipelineManager.registry(schemaName, providerServiceDataList, this.pipelines);
            if (state == null) {
                return;
            }
        } catch (Exception e) {
            logger.error("生成Schema失败！", e);
        }
        if (this.executionMonitor != null) {
            this.executionMonitor.onStateBuild(providerServiceDataList, state);
        }

        //部署
        long t = System.currentTimeMillis();
        try {
            this.deploy(state);
        } catch (Exception e) {
            logger.error("构建GraphQL Schema失败！", e);
        } finally {
            logger.info("构建Schema耗时 {}", System.currentTimeMillis() - t);
            GraphqlContextUtils.getGraphqlContext(schemaName).finish();
        }
    }

    /**
     * 发布，会聚合各provider的服务在一起
     *
     * @param registryState 当前更新状态
     */
    private void deploy(RegistryState registryState) {
        if (registryState == null) {
            return;
        }

        //当前schema状态，分别是：query, mutation, subscription
        SchemaBuildState schemaStatus = new SchemaBuildState();
        GraphQLObjectType.Builder queryBuilder = GraphQLObjectType.newObject().name(GraphqlConsts.QUERY);
        GraphQLObjectType.Builder mutationBuilder = GraphQLObjectType.newObject().name(GraphqlConsts.MUTATION);
        GraphQLObjectType.Builder subscriptionBuilder = GraphQLObjectType.newObject().name(GraphqlConsts.SUBSCRIPTION);

        //build query
        schemaStatus.hasQuery = this.buildQuery(registryState, queryBuilder);

        //build mutation
        schemaStatus.hasMutation = this.buildMutation(registryState, mutationBuilder);

        //build subscription
        schemaStatus.hasSubscription = false;//buildSubscription(registryState, subscriptionBuilder);

        //build inner provider
        this.buildInnerProvider(registryState);

        GraphQLSchema.Builder schemaBuilder = GraphQLSchema.newSchema();
        if (schemaStatus.hasQuery) {
            schemaBuilder.query(queryBuilder.build());
        }
        if (schemaStatus.hasMutation) {
            schemaBuilder.mutation(mutationBuilder.build());
        }
        if (schemaStatus.hasSubscription) {
            schemaBuilder.subscription(subscriptionBuilder.build());
        }

        Set<GraphQLDirective> directiveSet = this.directiveService.getDirectiveSet();
        if (!CollectionUtils.isEmpty(directiveSet)) {
            schemaBuilder.additionalDirectives(directiveSet);
        }
        GraphQLSchema graphQLSchema = schemaBuilder
                .codeRegistry(registryState.getCodeRegistry().build())
//                .additionalTypes(registryState.getGraphQLTypes())
                .build();

        //缓存Document，减少ql请求解析
        GraphQL graphQL = GraphQL.newGraphQL(graphQLSchema)
                                 .instrumentation(this.getInstrumentations())
                                 .subscriptionExecutionStrategy(new SubscriptionExecutionStrategy())
                                 .preparsedDocumentProvider(this.preparsedDocumentProvider)
                                 .build();
        this.graphqlInvocationConfigure.setGraphQL(registryState.getSchemaName(), graphQL);

        GraphqlContextUtils.CACHE.cleanUp();
        if (this.executionMonitor != null) {
            this.executionMonitor.onSchemaBuild(registryState, graphQL);
        }
    }

    private void buildInnerProvider(RegistryState registryState) {
        registryState.getModuleContextMap().entrySet()
                     .forEach((Map.Entry<String, GraphqlModuleContext> entry) -> {
                         GraphqlModuleContext moduleContext = entry.getValue();
                         if (moduleContext.getInnerProvider() != null && !CollectionUtils.isEmpty(moduleContext.getCodeRegistryMap())) {
                             moduleContext.getCodeRegistryMap().entrySet().forEach(codeRegistryMapEntry -> {
                                 String[] fields = codeRegistryMapEntry.getKey();
                                 FieldCoordinates coordinates = FieldCoordinates.coordinates(fields[0], fields[1]);
                                 registryState.getCodeRegistry().dataFetcher(coordinates, codeRegistryMapEntry.getValue());
                             });
                         }
                     });
    }

    private boolean buildSubscription(RegistryState registryState, GraphQLObjectType.Builder subscriptionBuilder) {
        SchemaBuildState state = new SchemaBuildState();
        String topicName = "testSubscription";
        DataFetcher subscriptionDataFetcher = this.getSubscriptionDataFetcher(topicName);
        GraphQLFieldDefinition.Builder groupSubscriptionBuilder = GraphQLFieldDefinition.newFieldDefinition()
                                                                                        .name(topicName)
                                                                                        .description("测试订阅功能")
                                                                                        .arguments(Lists.newArrayList(
                                                                                                GraphQLArgument.newArgument().name("id").type(GraphQLString)
                                                                                                               .build()
                                                                                        ))
                                                                                        .type(GraphQLTypeReference.typeRef("agent_Agent"));
        subscriptionBuilder.field(groupSubscriptionBuilder);
        state.hasSubscription = true;

        registryState.getCodeRegistry()
                     .dataFetcher(FieldCoordinates.coordinates(GraphqlConsts.SUBSCRIPTION, topicName), subscriptionDataFetcher)
        ;
        return state.hasSubscription;
    }

    @NotNull
    private DataFetcher getSubscriptionDataFetcher(String topicName) {
        SubscriptionDataFetcherProxy subscriptionDataFetcherProxy = new SubscriptionDataFetcherProxy();
        subscriptionDataFetcherProxy.setTopicName(topicName);
        return subscriptionDataFetcherProxy;
    }

    private boolean buildMutation(RegistryState registryState, GraphQLObjectType.Builder mutationBuilder) {
        SchemaBuildState state = new SchemaBuildState();
        Map<String, List<GraphQLFieldDefinition>> moduleMutionFieldDefinitionMap = registryState.getModuleMutationFieldDefinitionMap();
        if (!CollectionUtils.isEmpty(moduleMutionFieldDefinitionMap)) {
            moduleMutionFieldDefinitionMap.entrySet().forEach(entry -> {
                String moduleName = entry.getKey();
                List<GraphQLFieldDefinition> fieldDefinitions = entry.getValue();
                if (CollectionUtils.isEmpty(fieldDefinitions)) {
                    return;
                }
                String mutationTypeName = GraphqlConsts.STR_M + moduleName.toUpperCase();
                GraphQLObjectType moduleMutationOutType = GraphQLObjectType
                        .newObject()
                        .name(mutationTypeName)
                        .fields(fieldDefinitions)
                        .build();
                GraphQLFieldDefinition.Builder moduleMutationBuilder = GraphQLFieldDefinition.newFieldDefinition()
                                                                                             .name(moduleName)
                                                                                             .type(moduleMutationOutType);

                FieldCoordinates coordinates = FieldCoordinates.coordinates(GraphqlConsts.MUTATION, moduleName);
                DataFetcher dataFetcher = environment -> Maps.newHashMap();
                registryState.getCodeRegistry().dataFetcher(coordinates, dataFetcher);

                mutationBuilder.field(moduleMutationBuilder);
                state.hasMutation = true;
                registryState.addGraphQLType(mutationTypeName + GraphqlConsts.STR_DOT + moduleName, moduleMutationOutType);
            });
        }
        return state.hasMutation;
    }

    private boolean buildQuery(RegistryState registryState, GraphQLObjectType.Builder queryBuilder) {
        SchemaBuildState state = new SchemaBuildState();
        Map<String, List<GraphQLFieldDefinition>> moduleQueryFieldDefinitionMap = registryState.getModuleQueryFieldDefinitionMap();
        if (!CollectionUtils.isEmpty(moduleQueryFieldDefinitionMap)) {
            moduleQueryFieldDefinitionMap.entrySet().forEach(entry -> {
                String moduleName = entry.getKey();
                List<GraphQLFieldDefinition> fieldDefinitions = entry.getValue();
                if (CollectionUtils.isEmpty(fieldDefinitions)) {
                    return;
                }
                GraphQLObjectType moduleQueryOutType = GraphQLObjectType
                        .newObject()
                        .name(moduleName.toUpperCase())
                        .fields(fieldDefinitions)
                        .build();
                GraphQLFieldDefinition.Builder moduleQueryBuilder = GraphQLFieldDefinition.newFieldDefinition()
                                                                                          .name(moduleName)
                                                                                          .type(moduleQueryOutType);
                FieldCoordinates coordinates = FieldCoordinates.coordinates(GraphqlConsts.QUERY, moduleName);
                DataFetcher dataFetcher = environment -> Maps.newHashMap();
                registryState.getCodeRegistry().dataFetcher(coordinates, dataFetcher);

                queryBuilder.field(moduleQueryBuilder);
                state.hasQuery = true;
                registryState.addGraphQLType(moduleName.toUpperCase() + GraphqlConsts.STR_DOT + moduleName, moduleQueryOutType);
            });
        }
        return state.hasQuery;
    }

    private Instrumentation getInstrumentations() {
        MaxQueryDepthInstrumentation maxQueryDepthInstrumentation = new MaxQueryDepthInstrumentation(this.graphqlQueryMaxDepth);
        List<Instrumentation> instrumentations = Lists.newArrayList(maxQueryDepthInstrumentation);
        if (this.executionMonitor != null) {
            instrumentations.add(this.executionMonitor);
        }
        return new ChainedInstrumentation(instrumentations);
    }

    /**
     * 构建中的Schema状态
     */
    private class SchemaBuildState {

        boolean hasQuery = false;
        boolean hasMutation = false;
        boolean hasSubscription = false;

    }

}
