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
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!prod")
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
            var companyAddress = new Address(
                    "Avenida Milão", "151", "25930790", "Magé", "RJ"
            );

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
                    "Lucas SantAnna",
                    "11366653742",
                    "CTO",
                    "kronos.time.tech.solutions@gmail.com",
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

        } else {
            log.info("Dados iniciais já existem. Ignorando a carga.");
        }
    }
}