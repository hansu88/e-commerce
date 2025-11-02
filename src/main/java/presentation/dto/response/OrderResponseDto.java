package presentation.dto.response;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderResponseDto {
    private Long orderId;
    private String status;
    private Long appliedCouponId; // optional
}