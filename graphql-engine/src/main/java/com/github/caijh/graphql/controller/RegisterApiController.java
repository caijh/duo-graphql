package com.github.caijh.graphql.controller;

import com.github.caijh.graphql.provider.dto.BaseResponse;
import com.github.caijh.graphql.provider.dto.TpDocGraphqlProviderServiceInfo;
import com.github.caijh.graphql.register.GraphqlRegister;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author xuwenzhen
 * @chapter Graphql引擎
 * @section Provider注册接口
 * @date 2019/4/9
 */
@RestController
@RequestMapping("/api/register")
public class RegisterApiController {

    @Autowired
    private GraphqlRegister<TpDocGraphqlProviderServiceInfo> graphqlRegister;

    /**
     * 注册远端数据供应商（Duo-Doc项目）
     *
     * @return
     */
    @PostMapping("/tpdoc")
    public BaseResponse registerWithTpDoc(@RequestBody TpDocGraphqlProviderServiceInfo request) {
        this.graphqlRegister.register(request);
        return BaseResponse.success();
    }

    /**
     * Ping
     *
     * @return
     */
    @GetMapping("/ping")
    public BaseResponse<String> ping() {
        return BaseResponse.success("success");
    }

}
