package com.flea.market.ad;

import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public final class AdSpecifications {

    private AdSpecifications() {
    }

    public static Specification<Ad> hasStatus(AdStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Ad> titleContains(String title) {
        return (root, query, cb) -> title == null || title.isBlank()
                ? null
                : cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase() + "%");
    }

    public static Specification<Ad> hasCategory(String category) {
        return (root, query, cb) -> category == null || category.isBlank()
                ? null
                : cb.equal(cb.lower(root.get("category")), category.toLowerCase());
    }

    public static Specification<Ad> priceGte(BigDecimal minPrice) {
        return (root, query, cb) -> minPrice == null
                ? null
                : cb.and(cb.isTrue(root.get("priceIsNumeric")),
                        cb.greaterThanOrEqualTo(root.get("price"), minPrice));
    }

    public static Specification<Ad> priceLte(BigDecimal maxPrice) {
        return (root, query, cb) -> maxPrice == null
                ? null
                : cb.and(cb.isTrue(root.get("priceIsNumeric")),
                        cb.lessThanOrEqualTo(root.get("price"), maxPrice));
    }

    public static Specification<Ad> hasAuthor(String login) {
        return (root, query, cb) -> login == null || login.isBlank()
                ? null
                : cb.equal(root.get("author").get("login"), login);
    }
}
