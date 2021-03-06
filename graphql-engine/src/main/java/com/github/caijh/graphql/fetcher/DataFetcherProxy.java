package com.github.caijh.graphql.fetcher;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.caijh.graphql.core.BatchDataFetcherData;
import com.github.caijh.graphql.core.DataFetcherData;
import com.github.caijh.graphql.core.GraphqlConsts;
import com.github.caijh.graphql.core.UserExecutionContext;
import com.github.caijh.graphql.core.config.GraphqlProviderConfigure;
import com.github.caijh.graphql.core.exception.GraphqlInvocationException;
import com.github.caijh.graphql.core.util.DataFetchUtils;
import com.github.caijh.graphql.core.util.GraphqlContextUtils;
import com.github.caijh.graphql.core.util.GraphqlTypeUtils;
import com.github.caijh.graphql.core.util.OkHttpUtils;
import com.github.caijh.graphql.core.util.SelectionUtils;
import com.github.caijh.graphql.fetcher.batcher.BatchDataFetcherProxy;
import com.github.caijh.graphql.fetcher.batcher.BatchLoader;
import com.github.caijh.graphql.provider.BaseDataFetcher;
import com.github.caijh.graphql.provider.ValueUtils;
import com.github.caijh.graphql.provider.dto.TpDocGraphqlProviderServiceInfo;
import com.github.caijh.graphql.provider.dto.provider.Api;
import com.github.caijh.graphql.provider.dto.provider.EntityRef;
import com.github.caijh.graphql.register.JsonService;
import com.github.caijh.graphql.service.DirectiveService;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import graphql.language.Directive;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLOutputType;
import okhttp3.HttpUrl;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * ???????????????????????????????????????
 *
 * @author xuwenzhen
 * @date 2019/4/9
 */
public class DataFetcherProxy extends BaseDataFetcher {

    private static final Logger logger = LoggerFactory.getLogger(DataFetcherProxy.class);
    private static final RequestBody EMPTY_REQUEST_BODY = RequestBody.create(new byte[0]);

    /**
     * ???????????????????????????????????????Pattern
     */
    private static final Map<String, Pattern> PATH_PARAM_PATTERN_MAP = Maps.newConcurrentMap();

    @Autowired
    private DirectiveService directiveService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OkHttpClient okHttpClient;

    @Autowired
    private JsonService jsonService;

    @Autowired
    private GraphqlProviderConfigure graphqlProviderConfigure;

    protected Api api;

    protected TpDocGraphqlProviderServiceInfo provider;

    /**
     * api?????????????????? => ?????????
     */
    private Map<String, Object> selectionConstMap;

    /**
     * api???????????? => type???????????????
     */
    private Map<String, String> selectionApiParamMap;

    /**
     * ???????????????????????????
     *
     * @demo userId
     */
    private String graphqlProviderName;

    /**
     * ????????????????????????????????????jsonPath?????????????????? TpdocBaseRestProvider????????????????????????
     *
     * @demo $.data
     */
    private String dataPath;

    public DataFetcherProxy(Api api, TpDocGraphqlProviderServiceInfo provider) {
        this.api = api;
        this.provider = provider;
    }

    @Override
    protected Object getData(DataFetchingEnvironment environment) {
        //???????????????????????????
        DataFetcherData dataFetcherData = SelectionUtils.analyseGql(environment);
        UserExecutionContext executionContext = environment.getContext();

        String path = environment.getExecutionStepInfo().getPath().toString();
        int index = path.indexOf(GraphqlConsts.ARRAY_INDEX_START);
        if (index > -1) {
            int end = path.indexOf(GraphqlConsts.ARRAY_INDEX_END);
            String indexStr = path.substring(index + 1, end);
            path = path.substring(0, index) + path.substring(end + 1);
            BatchLoader batcherLoader = executionContext.getBatchLoader(path);
            if (batcherLoader != null) {
                Object data = batcherLoader.get(Integer.parseInt(indexStr));
                //????????????
                this.processDirective(environment, dataFetcherData, data);
                return data;
            }
        }

        List<String> selections = dataFetcherData.getSelections();

        Map<String, Object> params = this.getApiParamValues(environment, this.api);
        String body = null;
        if (params != null) {
            body = this.getApiObject(this.getProviderServer(), this.api, selections, params, executionContext.getHeaders());
        }
        Object data;
        if (StringUtils.isEmpty(this.dataPath)) {
            data = this.jsonService.toObject(body);
        } else {
            data = this.jsonService.toObject(body, this.dataPath);
        }

        //????????????
        this.processDirective(environment, dataFetcherData, data);

        //???????????????????????????????????????????????????????????????????????????????????????
        this.doBatchFetch(environment, dataFetcherData, data);
        return data;
    }

    private void doBatchFetch(DataFetchingEnvironment environment, DataFetcherData dataFetcherData, Object data) {
        Map<String, BatchDataFetcherData> batchDataFetcherDataMap = dataFetcherData.getBatchDataFetcherDataMap();
        if (CollectionUtils.isEmpty(batchDataFetcherDataMap)) {
            return;
        }

        UserExecutionContext executionContext = environment.getContext();
        batchDataFetcherDataMap.entrySet().forEach(entry -> {
            BatchDataFetcherData batchDataFetcherData = entry.getValue();
            if (batchDataFetcherData == null) {
                return;
            }

            Map<String, List<Object>> params = Maps.newHashMap();
            DataFetcherProxy originDataFetcher = batchDataFetcherData.getDataFetcher();

            Api batchApi = batchDataFetcherData.getApi();
            Set<String> requestSet = Sets.newHashSet();
            batchApi.getRequestParams().forEach(rp -> requestSet.add(rp.getName()));
            originDataFetcher.selectionApiParamMap.entrySet().forEach(paramMapEntry -> {
                String paramName = paramMapEntry.getKey();
                String fieldName = paramMapEntry.getValue();
                if (!requestSet.contains(paramName)) {
                    paramName += GraphqlConsts.STR_S;
                }
                if (!requestSet.contains(paramName)) {
                    throw new GraphqlInvocationException(batchApi + "?????????????????????????????????" + paramName);
                }
                Object values = this.getParamValues(environment, data, fieldName, batchDataFetcherData.getFieldPath(), false);
                params.put(paramName, (List<Object>) values);
            });
            //??????????????????
            Map<String, Object> selectionConstMap = originDataFetcher.getSelectionConstMap();
            if (!CollectionUtils.isEmpty(selectionConstMap)) {
                selectionConstMap.forEach((paramName, value) -> params.put(paramName, Lists.newArrayList(value)));
            }

            BatchDataFetcherProxy batchDataFetcherProxy = new BatchDataFetcherProxy();
            batchDataFetcherProxy.setServiceInfo(batchDataFetcherData.getContextModule().getProvider());
            batchDataFetcherProxy.setApi(batchApi);
            batchDataFetcherProxy.setParams(params);
            List<String> selections = batchDataFetcherData.getSelections();
            if (!CollectionUtils.isEmpty(selections) && !selections.contains(GraphqlConsts.STR_ID_LOWER)) {
                selections.add(GraphqlConsts.STR_ID_LOWER);
            }
            batchDataFetcherProxy.setSelections(selections);
            batchDataFetcherProxy.setRefIdsMerge(originDataFetcher.getApi() == batchApi);
            batchDataFetcherProxy.setPath(entry.getKey());
            GraphqlContextUtils.getApplicationContext().getAutowireCapableBeanFactory().autowireBean(batchDataFetcherProxy);
            //????????????
            executionContext.bathFetch(batchDataFetcherProxy);
        });
    }

    private Object getParamValues(DataFetchingEnvironment environment, Object data, String fieldName, String fieldPath, boolean overList) {
        if (data == null) {
            return overList ? Lists.newArrayList() : null;
        }
        if (data instanceof Map) {
            //????????????Map
            if (!StringUtils.isEmpty(fieldPath)) {
                int index = fieldPath.indexOf(GraphqlConsts.PATH_SPLITTER);
                String path;
                String subFieldPath = null;
                if (index > -1) {
                    path = fieldPath.substring(0, index);
                    subFieldPath = fieldPath.substring(index + 1);
                } else {
                    path = fieldPath;
                }
                Object fieldValue = ((Map) data).get(path);
                return this.getParamValues(environment, fieldValue, fieldName, subFieldPath, overList);
            }
            return ValueUtils.getParamValue(environment, data, null, fieldName);
        } else if (data instanceof List) {
            //??????????????????
            List<Object> values = Lists.newArrayList();
            ((List) data).forEach(item -> {
                Object itemValue = this.getParamValues(environment, item, fieldName, fieldPath, true);
                values.add(itemValue);
            });
            return values;
        }
        throw new GraphqlInvocationException("??????????????????");
    }

    protected String getProviderServer() {
        if (this.graphqlProviderConfigure == null) {
            return this.provider.getServer();
        }
        String providerService = this.graphqlProviderConfigure.getProviderService(this.provider.getAppId());
        if (providerService != null) {
            return providerService;
        }
        return this.provider.getServer();
    }

    /**
     * ???????????????????????????
     *
     * @param environment     ???????????????
     * @param dataFetcherData ??????????????????
     * @param data            ????????????
     */
    private void processDirective(DataFetchingEnvironment environment, DataFetcherData dataFetcherData, Object data) {
        Map<String, List<Directive>> fieldDirectiveMap = dataFetcherData.getFieldDirectives();
        if (CollectionUtils.isEmpty(fieldDirectiveMap)) {
            return;
        }
        this.directiveService.processDirective(environment, data, fieldDirectiveMap);
    }

    private String getObject(HttpMethod method, HttpUrl.Builder urlBuilder, Map<String, String> headers, RequestBody requestBody) {
        Request.Builder requestBuilder = OkHttpUtils.getRestFulRequestBuilder(urlBuilder);

        if (!StringUtils.isEmpty(this.provider.getAppId())) {
            //??????Mesh??????????????????
            requestBuilder.addHeader(OkHttpUtils.HOST, this.provider.getAppId());
        }
        if (!CollectionUtils.isEmpty(headers)) {
            headers.entrySet().forEach(entry -> requestBuilder.addHeader(entry.getKey(), entry.getValue()));
        }

        if (HttpMethod.POST == method) {
            requestBuilder.post(requestBody);
        } else if (HttpMethod.DELETE == method) {
            requestBuilder.delete(requestBody);
        } else if (HttpMethod.PATCH == method) {
            requestBuilder.patch(requestBody);
        } else if (HttpMethod.PUT == method) {
            requestBuilder.put(requestBody);
        }

        Request request = requestBuilder.build();
        long t1 = System.currentTimeMillis();
        try (Response response = this.okHttpClient.newCall(request).execute()) {
            if (response.code() != HttpStatus.OK.value()) {
                throw new GraphqlInvocationException("?????????????????????query:" + request + "???status:" + response.code());
            }

            try (ResponseBody responseBody = response.body()) {
                if (responseBody == null) {
                    return null;
                }
                return responseBody.string();
            }
        } catch (IOException e) {
            throw new GraphqlInvocationException("???????????????" + e.getMessage() + "," + request, e);
        } finally {
            logger.info("{}, ?????? {}", urlBuilder, System.currentTimeMillis() - t1);
        }
    }

    protected String getApiObject(
            String server,
            Api api,
            List<String> selections,
            Map<String, Object> gqlParams,
            Map<String, String> headers
    ) {
        String url = server + api.getPaths().get(0);

        List<EntityRef> requestParams = api.getRequestParams();
        EntityRef requestBodyParam = null;
        List<EntityRef> urlParams = Lists.newArrayList();
        if (!CollectionUtils.isEmpty(requestParams)) {
            for (EntityRef param : api.getRequestParams()) {
                if (GraphqlTypeUtils.REQUEST_BODY.equals(param.getAnnotation())) {
                    // @RequestBody
                    requestBodyParam = param;
                    continue;
                }
                if (!GraphqlTypeUtils.PATH_VARIABLE.equals(param.getAnnotation())) {
                    urlParams.add(param);
                    continue;
                }
                String name = param.getName();
                url = this.setPathParam(url, name, gqlParams.get(name));
            }
        }

        HttpUrl httpUrl = HttpUrl.parse(url);
        if (httpUrl == null) {
            throw new GraphqlInvocationException("?????????????????????url??????:" + url);
        }

        HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
        if (!CollectionUtils.isEmpty(urlParams)) {
            urlParams
                    .forEach(param -> {
                        String paramName = param.getName();
                        Object paramValue = gqlParams.get(paramName);
                        if (paramValue == null) {
                            return;
                        }
                        if (paramValue instanceof Map) {
                            //Map
                            Map<String, Object> paramMap = (Map<String, Object>) paramValue;
                            for (Map.Entry<String, Object> entry : paramMap.entrySet()) {
                                this.addUrlParams(urlBuilder, entry.getKey(), entry.getValue());
                            }
                        } else {
                            this.addUrlParams(urlBuilder, paramName, paramValue);
                        }
                    });
        }

        //????????????
        HttpMethod method = DataFetchUtils.getHttpMethod(api);
        String selectionStr = null;
        if (!CollectionUtils.isEmpty(selections)) {
            selectionStr = OkHttpUtils.formatSelections(selections);
            urlBuilder.addQueryParameter(GraphqlConsts.STR_SELECTIONS, selectionStr);
        }

        RequestBody requestBody;
        try {
            requestBody = this.getRequestBody(requestBodyParam, gqlParams);
        } catch (Exception e) {
            throw new GraphqlInvocationException("??????RequestBody?????????[" + method.name() + "]urlBuilder=" + urlBuilder + ",selections=" + selectionStr, e);
        }
        return this.getObject(method, urlBuilder, headers, requestBody);
    }

    private void addUrlParams(HttpUrl.Builder urlBuilder, String paramName, Object paramValue) {
        if (paramValue == null) {
            urlBuilder.addEncodedQueryParameter(paramName, GraphqlConsts.STR_EMPTY);
            return;
        }
        if (paramValue instanceof List) {
            ((List) paramValue).forEach(pv -> this.addUrlParams(urlBuilder, paramName, pv));
        } else {
            urlBuilder.addEncodedQueryParameter(paramName, paramValue.toString());
        }
    }

    private RequestBody getRequestBody(EntityRef requestBodyParam, Map<String, Object> gqlParams) throws JsonProcessingException {
        if (requestBodyParam == null) {
            return EMPTY_REQUEST_BODY;
        }
        String paramName = requestBodyParam.getName();
        Object val = gqlParams.get(paramName);
        if (val == null) {
            return EMPTY_REQUEST_BODY;
        }

        return MultipartBody.create(this.objectMapper.writeValueAsBytes(val));
    }

    private String setPathParam(String url, String paramName, Object paramValue) {
        Pattern pattern = getPathParamPattern(paramName);
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            url = matcher.replaceAll(paramValue.toString());
        }
        return url;
    }

    protected static Pattern getPathParamPattern(String pathParamName) {
        Pattern pathParamPattern = PATH_PARAM_PATTERN_MAP.get(pathParamName);
        if (pathParamPattern != null) {
            return pathParamPattern;
        }
        Pattern pattern = Pattern.compile("\\{\\s*" + pathParamName + "(\\s*:.*?)?\\}");
        PATH_PARAM_PATTERN_MAP.put(pathParamName, pattern);
        return pattern;
    }

    /**
     * ????????????Selection???????????????
     *
     * @param selection selection??????
     * @return selection???????????????
     */
    public String getParamName(String selection) {
        if (this.selectionApiParamMap == null) {
            return selection;
        }
        String mappingName = this.selectionApiParamMap.get(selection);
        return StringUtils.isEmpty(mappingName) ? selection : mappingName;
    }

    public void addExtraSelection(String apiParamName, String selectionName) {
        if (this.selectionApiParamMap == null) {
            this.selectionApiParamMap = Maps.newHashMap();
        }
        this.selectionApiParamMap.put(apiParamName, selectionName);
    }

    public void setExtraSelections(List<String> extraSelections) {
        if (CollectionUtils.isEmpty(extraSelections)) {
            return;
        }
        if (this.getDependencyFields() == null) {
            this.setDependencyFields(Lists.newArrayList());
        }
        this.selectionApiParamMap = Maps.newHashMap();
        extraSelections.forEach(selectionConfig -> {
            int i = selectionConfig.indexOf(GraphqlConsts.STR_MAP);
            String selection = selectionConfig;
            if (i == -1) {
                //?????????????????????
                i = selectionConfig.indexOf(GraphqlConsts.STR_EQ);
                if (i == -1) {
                    this.selectionApiParamMap.put(selectionConfig, selectionConfig);
                    this.addDependencyField(selectionConfig);
                } else {
                    //???????????????
                    selection = selectionConfig.substring(0, i);
                    String constValue = selectionConfig.substring(i + 1).trim();
                    this.addApiConstParam(selection, constValue);
                }
            } else {
                selection = selectionConfig.substring(0, i);
                this.addDependencyField(selection);
                this.selectionApiParamMap.put(selectionConfig.substring(i + 2), selection);
            }
        });
    }

    private void addApiConstParam(String selection, String constValue) {
        Optional<EntityRef> paramOption = this.api.getRequestParams().stream().filter(param -> param.getName().equals(selection)).findFirst();
        if (!paramOption.isPresent()) {
            return;
        }
        if (this.selectionConstMap == null) {
            this.selectionConstMap = Maps.newHashMap();
        }
        this.selectionConstMap.put(selection, this.setConstValue(paramOption.get(), constValue));
    }

    private Object setConstValue(EntityRef entityRef, String constValue) {
        return DataFetchUtils.convertStringValue(entityRef.getEntityName(), constValue);
    }

    /**
     * ?????????????????????????????????????????????
     *
     * @param environment ???????????????
     * @param api         ??????
     * @return
     */
    private Map<String, Object> getApiParamValues(DataFetchingEnvironment environment, Api api) {
        Map<String, Object> apiParamValues = Maps.newHashMap();
        List<EntityRef> requestParams = api.getRequestParams();
        if (!CollectionUtils.isEmpty(requestParams)) {
            for (EntityRef param : api.getRequestParams()) {
                String paramName = param.getName();
                String fieldName = this.getParamName(paramName);
                Object paramValue = ValueUtils.getParamValue(environment, environment.getSource(), this.getSelectionConstMap(), fieldName);
                if (paramValue == null && param.isRequired()) {
                    //??????????????????
                    logger.warn("?????????{}??????????????????{}?????????", api, paramName);
                    return null;
                }
                apiParamValues.put(paramName, paramValue);
            }
        }

        return apiParamValues;
    }

    /**
     * ?????????????????????
     *
     * @return ???????????????
     */
    public Map<String, Object> getSelectionConstMap() {
        return this.selectionConstMap;
    }

    public String getGraphqlProviderName() {
        return this.graphqlProviderName;
    }

    public void setGraphqlProviderName(String graphqlProviderName) {
        this.graphqlProviderName = graphqlProviderName;
    }

    public Api getApi() {
        return this.api;
    }

    /**
     * ??????????????????
     *
     * @return ????????????
     */
    @Override
    public String getModuleName() {
        return this.api.getModuleName();
    }

    /**
     * ?????????DataFetcher?????????GraphQL??????
     *
     * @return GraphQL??????
     */
    @Override
    public GraphQLOutputType getResponseGraphqlType() {
        return null;
    }

    public TpDocGraphqlProviderServiceInfo getProvider() {
        return this.provider;
    }

    public void setDataPath(String dataPath) {
        this.dataPath = dataPath;
    }

    public String getDataPath() {
        return this.dataPath;
    }

}
