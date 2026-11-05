package pe.yape.training.product;

import java.math.BigDecimal;

public record Product(Long id, String name, BigDecimal price, Long ownerCustomerId) {
}
