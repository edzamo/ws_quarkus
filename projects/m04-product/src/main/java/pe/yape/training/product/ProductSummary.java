package pe.yape.training.product;

public record ProductSummary(
    Long id,
    String name,
    String description,
    String ownerCustomerName,
    String ownerCustomerTier,
    double riskScore
) {

  public ProductSummary(Product p, Customer c, RiskScore r) {
    this(p.id(), p.name(), p.description(), c.name(), c.tier(), r.score());
  }
}

