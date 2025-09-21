package com.kts.kronos;

import com.kts.kronos.adapter.in.web.dto.company.Location;
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


        if (companyProvider.findByCnpj("00000000000000").isEmpty()) {
            var companyAddress = addressLookupProvider.lookup("25930790")
                    .withNumber("151");

            // 1. Criar e salvar a empresa
            var companyLocation = new Location(-22.633119552674476, -43.171951583299034);
            var company = new Company(
                    "Kronos Tech Solutions",
                    "00000000000000",
                    "santanna.dev.94@gmail.com",
                    companyAddress,
                    companyLocation

            );
            companyProvider.save(company);
            log.info("Empresa 'Kronos Tech Solutions' criada com sucesso.");

            var hashedPassword = passwordEncoder.encode("Tec2659*");

            // 2. Criar funcionário e usuário para a role CTO
            var ctoEmployee = new Employee(
                    "Lucas Barbosa (CTO)",
                    "11366653742",
                    "CTO",
                    "cto@kronos.com.br",
                    1.00,
                    "21964032474",
                    companyAddress,
                    company.companyId()
            );
            employeeProvider.save(ctoEmployee);
            var ctoUser = new User(
                    "CTO",
                    hashedPassword,
                    Role.CTO,
                    ctoEmployee.employeeId()
            );
            userProvider.save(ctoUser);
            log.info("Usuário 'CTO' criado com sucesso.");

            // 3. Criar funcionário e usuário para a role MANAGER
            var managerEmployee = new Employee(
                    "Lucas Barbosa (MANAGER)",
                    "11366653743",
                    "MANAGER",
                    "manager@kronos.com.br",
                    1.00,
                    "21964032475",
                    companyAddress,
                    company.companyId()
            );
            employeeProvider.save(managerEmployee);
            var managerUser = new User(
                    "MANAGER",
                    hashedPassword,
                    Role.MANAGER,
                    managerEmployee.employeeId()
            );
            userProvider.save(managerUser);
            log.info("Usuário 'MANAGER' criado com sucesso.");

            // 4. Criar funcionário e usuário para a role PARTNER
            var partnerEmployee = new Employee(
                    "Lucas Barbosa (PARTNER)",
                    "11366653744",
                    "PARTNER",
                    "partner@kronos.com.br",
                    1.00,
                    "21964032476",
                    companyAddress,
                    company.companyId()
            );
            employeeProvider.save(partnerEmployee);
            var partnerUser = new User(
                    "PARTNER",
                    hashedPassword,
                    Role.PARTNER,
                    partnerEmployee.employeeId()
            );
            userProvider.save(partnerUser);
            log.info("Usuário 'PARTNER' criado com sucesso.");

        } else {
            log.info("Dados iniciais já existem. Ignorando a carga.");
        }
    }
}