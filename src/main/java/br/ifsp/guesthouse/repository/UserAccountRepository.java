
package br.ifsp.guesthouse.repository;

import br.ifsp.guesthouse.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAccountRepository
        extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByEmail(String email);

    Optional<UserAccount> findByCpfAndEmail(String cpf, String email);

    boolean existsByEmail(String email);

    boolean existsByCpf(String cpf);
}
