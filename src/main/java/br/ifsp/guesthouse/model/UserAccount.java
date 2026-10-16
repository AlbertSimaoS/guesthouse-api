package br.ifsp.guesthouse.model;

import br.ifsp.guesthouse.model.enums.Profile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "user_account")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank(message = "CPF is required")
    @Pattern(
            regexp = "\\d{11}",
            message = "CPF must contain exactly 11 digits"
    )
    @Column(nullable = false, unique = true, length = 11)
    private String cpf;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @NotBlank(message = "Telephone is required")
    @Pattern(
            regexp = "^\\+?[1-9]\\d{1,14}$",
            message = "Telephone number must be in a valid international format"
    )
    @Column(nullable = false, length = 16)
    private String telephone;

    @NotBlank(message = "Password is required")
    @Size(max = 100, message = "Password hash must not exceed 100 characters")
    @Column(nullable = false, length = 100)
    private String password;

    @NotNull(message = "Profile is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Profile profile = Profile.GUEST;

    public UserAccount() {
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Profile getProfile() {
        return profile;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
    }
}