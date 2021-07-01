package com.github.caijh.graphql.dto.resp;

import java.util.List;

/**
 * @author xuwenzhen
 * @date 2019/10/12
 */
public class BasePagedList<T> {

    /**
     * 总记录数
     */
    private Integer total;

    /**
     * 数据列表
     */
    private List<T> list;

    public Integer getTotal() {
        return this.total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public List<T> getList() {
        return this.list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }

}
