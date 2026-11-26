package com.hogwarts.library;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import liquibase.change.core.AddForeignKeyConstraintChange;
import liquibase.change.core.AddUniqueConstraintChange;
import liquibase.change.core.CreateIndexChange;
import liquibase.changelog.ChangeLogParameters;
import liquibase.parser.ChangeLogParserFactory;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ForeignKeyIndexOrderTest {

    @Test
    void createsChildIndexesBeforeForeignKeys() throws Exception {
        String path = "db/changelog/db.changelog-master.yaml";
        try (var resources = new ClassLoaderResourceAccessor()) {
            var parser = ChangeLogParserFactory.getInstance().getParser(path, resources);
            var changelog = parser.parse(path, new ChangeLogParameters(), resources);
            for (var changeSet : changelog.getChangeSets()) {
                Map<String, Set<String>> indexedColumns = new HashMap<>();
                for (var change : changeSet.getChanges()) {
                    if (change instanceof CreateIndexChange index) {
                        indexedColumns.computeIfAbsent(index.getTableName(), table -> new HashSet<>())
                                .add(index.getColumns().getFirst().getName());
                    } else if (change instanceof AddUniqueConstraintChange unique) {
                        indexedColumns.computeIfAbsent(unique.getTableName(), table -> new HashSet<>())
                                .add(unique.getColumnNames().split(",", 2)[0].trim());
                    } else if (change instanceof AddForeignKeyConstraintChange foreignKey) {
                        assertThat(indexedColumns.getOrDefault(foreignKey.getBaseTableName(), Set.of()))
                                .as("Leading index column for %s in %s", foreignKey.getConstraintName(), changeSet.getId())
                                .contains(foreignKey.getBaseColumnNames());
                    }
                }
            }
        }
    }
}
