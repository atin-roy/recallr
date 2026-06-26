package com.atinroy.recallr.user;

import com.atinroy.recallr.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
public class User extends BaseEntity {
    @Setter
    @Column(unique = true)
    private String email;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserProvider> providers = new HashSet<>(); //initialized eagerly so we never get a NullPointerException when calling .getProviders()

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Set<Role> roles = new HashSet<>(Set.of(Role.USER));

    public void addProvider(UserProvider provider) {
        provider.setUser(this); // maintain both sides of the relationship
        this.providers.add(provider);
    }

    public void removeProvider(UserProvider provider) {
        this.providers.remove(provider);
        provider.setUser(null);
    }
}
