package com.kts.kronos;

import com.kts.kronos.application.port.out.provider.AddressLookupProvider;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.domain.model.Address;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.enuns.Role;
import com.kts.kronos.domain.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InitialDataLoader implements CommandLineRunner {

    private final CompanyProvider companyProvider;
    private final EmployeeProvider employeeProvider;
    private final UserProvider userProvider;
    private final AddressLookupProvider addressLookupProvider;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        log.info("Iniciando a carga de dados iniciais...");

        // 1. Criar e salvar uma empresa
        if (companyProvider.findByCnpj("00000000000000").isEmpty()) {
            Address companyAddress = addressLookupProvider.lookup("25930790")
                    .withNumber("151");
            Company company = new Company(
                    "Kronos Tech Solutions",
                    "00000000000000",
                    "santanna.dev.94@gmail.com",
                    companyAddress
            );
            companyProvider.save(company);
            log.info("Empresa 'Kronos Tech Solutions' criada com sucesso.");

            // 2. Criar e salvar um funcionário associado à empresa
            Employee employee = new Employee(
                    "Lucas Barbosa",
                    "11366653742",
                    "CTO",
                    "santanna.dev.94@gmail.com",
                    1.00,
                    "21964032474",
                    companyAddress,
                    company.companyId()
            );
            employeeProvider.save(employee);
            log.info("Funcionário 'Lucas Barbosa' criado com sucesso.");

            // 3. Criar e salvar um usuário associado ao funcionário com a função 'CTO'
            String hashedPassword = passwordEncoder.encode("Tec2659*");
            User user = new User(
                    "lucas.barbosa",
                    hashedPassword,
                    Role.MANAGER,
                    employee.employeeId()
            );
            userProvider.save(user);
            log.info("Usuário 'luis.barbosa' criado com sucesso com a role CTO.");

        } else {
            log.info("Dados iniciais já existem. Ignorando a carga.");
        }
    }
}