package mcc.survey.creator.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "system_config", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"config_key"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_key", nullable = false, unique = true)
    private String configKey;

    @Column(name = "config_value", nullable = false, length = 1024) // Increased length for potentially long encrypted values
    private String configValue;

    @Column(name = "is_encrypted", nullable = false)
    private boolean encrypted; // Renamed from 'encrypted' to 'isEncrypted' for clarity with lombok, but column name remains 'is_encrypted'
}
