package org.clockworx.werewolf.database;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

/**
 * Custom Hibernate naming strategy that applies a prefix to all table names.
 * This allows multiple plugins to use the same database without table name conflicts.
 */
public class PrefixPhysicalNamingStrategy implements PhysicalNamingStrategy {
    
    private final String prefix;
    
    /**
     * Creates a new PrefixPhysicalNamingStrategy with the specified prefix.
     *
     * @param prefix The prefix to apply to all table names (can be empty string).
     */
    public PrefixPhysicalNamingStrategy(String prefix) {
        this.prefix = prefix != null ? prefix : "";
    }
    
    @Override
    public Identifier toPhysicalCatalogName(Identifier name, JdbcEnvironment jdbcEnvironment) {
        return name;
    }
    
    @Override
    public Identifier toPhysicalSchemaName(Identifier name, JdbcEnvironment jdbcEnvironment) {
        return name;
    }
    
    @Override
    public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment jdbcEnvironment) {
        if (prefix.isEmpty()) {
            return name;
        }
        String tableName = prefix + name.getText();
        return Identifier.toIdentifier(tableName, name.isQuoted());
    }
    
    @Override
    public Identifier toPhysicalSequenceName(Identifier name, JdbcEnvironment jdbcEnvironment) {
        return name;
    }
    
    @Override
    public Identifier toPhysicalColumnName(Identifier name, JdbcEnvironment jdbcEnvironment) {
        return name;
    }
}

