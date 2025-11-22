package com.hhplus.ecommerce.application.command.point;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UsePointCommand {
    private Long userId;
    private Integer amount;
    private String reason;
}
