package dev.efrain.gilacommerce.repository;

import dev.efrain.gilacommerce.entity.Product;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

class ProductRepositoryImpl implements ProductRepositoryCustom {

    private static final Map<String, String> SORTABLE_COLUMNS = Map.of(
            "sku", "sku",
            "name", "name",
            "category", "category",
            "price", "price",
            "stock", "stock",
            "updatedAt", "updated_at"
    );

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<Product> search(String name, Pageable pageable) {
        String column = pageable.getSort().stream().findFirst()
                .map(order -> SORTABLE_COLUMNS.getOrDefault(order.getProperty(), "id"))
                .orElse("id");
        String direction = pageable.getSort().stream().findFirst()
                .map(order -> order.isAscending() ? "ASC" : "DESC")
                .orElse("ASC");

        String sql = "SELECT * FROM product WHERE deleted_at IS NULL "
                + "AND (CAST(:name AS varchar) IS NULL OR name ILIKE CONCAT('%', CAST(:name AS varchar), '%')) "
                + "ORDER BY " + column + " " + direction;
        String countSql = "SELECT count(*) FROM product WHERE deleted_at IS NULL "
                + "AND (CAST(:name AS varchar) IS NULL OR name ILIKE CONCAT('%', CAST(:name AS varchar), '%'))";

        List<Product> content = entityManager.createNativeQuery(sql, Product.class)
                .setParameter("name", name)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();
        long total = ((Number) entityManager.createNativeQuery(countSql)
                .setParameter("name", name)
                .getSingleResult()).longValue();

        return new PageImpl<>(content, pageable, total);
    }
}
