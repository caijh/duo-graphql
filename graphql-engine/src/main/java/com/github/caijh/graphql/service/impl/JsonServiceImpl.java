package com.github.caijh.graphql.service.impl;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.github.caijh.graphql.core.GraphqlConsts;
import com.github.caijh.graphql.core.exception.GraphqlInvocationException;
import com.github.caijh.graphql.register.JsonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * @author xuwenzhen
 * @date 2019/7/31
 */
@Service
public class JsonServiceImpl implements JsonService {

    private static final String JSON_SERIALIZE_ERROR = "JSON字符串系列化失败：";
    private static final String JSON_DESERIALIZE_ERROR = "返系列化JSON失败";

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 将Json字符串转换为对象
     *
     * @param jsonStr Json字符串
     * @return 对象
     */
    @Override
    public Object toObject(String jsonStr) {
        if (StringUtils.isEmpty(jsonStr)) {
            return null;
        }
        char firstChar = jsonStr.charAt(0);
        if (firstChar == GraphqlConsts.CHAR_OBJ_START) {
            //转换成Map
            try {
                return this.objectMapper.readValue(jsonStr, Map.class);
            } catch (IOException e) {
                throw new GraphqlInvocationException(JSON_SERIALIZE_ERROR + jsonStr, e);
            }
        } else if (firstChar == GraphqlConsts.CHAR_ARRAY_START) {
            //转换成list
            try {
                return this.objectMapper.readValue(jsonStr, List.class);
            } catch (IOException e) {
                throw new GraphqlInvocationException(JSON_SERIALIZE_ERROR + jsonStr, e);
            }
        } else if (firstChar == GraphqlConsts.CHAR_SYH) {
            //转换成String
            try {
                return this.objectMapper.readValue(jsonStr, String.class);
            } catch (IOException e) {
                throw new GraphqlInvocationException(JSON_SERIALIZE_ERROR + jsonStr, e);
            }
        }

        return jsonStr;
    }

    @Override
    public JsonNode toObject(String jsonStr, String dataPath) {
        if (StringUtils.isEmpty(jsonStr)) {
            return null;
        }
        JsonNode nodeTree;
        try {
            nodeTree = this.objectMapper.readTree(jsonStr);
        } catch (IOException e) {
            throw new GraphqlInvocationException(JSON_SERIALIZE_ERROR + jsonStr, e);
        }
        if (nodeTree == null) {
            return null;
        }
        return nodeTree.at(dataPath);
    }

    @Override
    public <T> T toObject(String jsonStr, Class<T> clazz) {
        if (StringUtils.isEmpty(jsonStr)) {
            return null;
        }
        char firstChar = jsonStr.charAt(0);
        if (firstChar == GraphqlConsts.CHAR_OBJ_START) {
            //转换成Map
            try {
                return this.objectMapper.readValue(jsonStr, clazz);
            } catch (IOException e) {
                throw new GraphqlInvocationException(JSON_SERIALIZE_ERROR + jsonStr, e);
            }
        } else if (firstChar == GraphqlConsts.CHAR_ARRAY_START) {
            //转换成list
            CollectionType collectionType = this.objectMapper.getTypeFactory().constructCollectionType(List.class, clazz);
            try {
                return this.objectMapper.readValue(jsonStr, collectionType);
            } catch (IOException e) {
                throw new GraphqlInvocationException(JSON_SERIALIZE_ERROR + jsonStr, e);
            }
        } else if (firstChar == GraphqlConsts.CHAR_SYH) {
            //转换成String
            return null;
        }

        throw new GraphqlInvocationException(JSON_SERIALIZE_ERROR + jsonStr);
    }

    @Override
    public String toJsonString(Object json) {
        try {
            return this.objectMapper.writeValueAsString(json);
        } catch (JsonProcessingException e) {
            throw new GraphqlInvocationException(JSON_DESERIALIZE_ERROR, e);
        }
    }

}
