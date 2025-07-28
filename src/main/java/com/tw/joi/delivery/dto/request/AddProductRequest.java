package com.tw.joi.delivery.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddProductRequest {

    private String userId;
    private String outletId;
    private String productId;

}
