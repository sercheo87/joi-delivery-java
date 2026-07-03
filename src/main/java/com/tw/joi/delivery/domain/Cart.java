package com.tw.joi.delivery.domain;

import lombok.*;

import java.util.ArrayList;
import java.util.List;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cart {

    private String cartId;
    private Outlet outlet;

    @Builder.Default
    private List<Product> products = new ArrayList<>();

    private User user;

}
