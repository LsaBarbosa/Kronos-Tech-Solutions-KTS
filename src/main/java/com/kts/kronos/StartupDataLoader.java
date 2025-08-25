package com.kts.kronos;

import com.kts.kronos.adapter.in.web.dto.address.AddressRequest;
import com.kts.kronos.adapter.in.web.dto.company.CreateCompanyRequest;
import com.kts.kronos.adapter.in.web.dto.employee.CreateEmployeeRequest;
import com.kts.kronos.adapter.in.web.dto.user.CreateUserRequest;
import com.kts.kronos.application.port.in.usecase.CompanyUseCase;
import com.kts.kronos.application.port.in.usecase.EmployeeUseCase;
import com.kts.kronos.application.port.in.usecase.UserUseCase;
import com.kts.kronos.domain.model.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.UUID;


@Component
@RequiredArgsConstructor
@Slf4j
public class StartupDataLoader implements CommandLineRunner {

    private final CompanyUseCase companyUseCase;
    private final EmployeeUseCase employeeUseCase;
    private final UserUseCase userUseCase;

    @Override
    public void run(String... args) throws Exception {
        log.info("Iniciando a criação de dados iniciais...");

        try {
            // 1. Crie uma nova empresa
            var companyRequest = new CreateCompanyRequest(
                    "Kronos Tech Solutions",
                    "00000000000000",
                    "contato@kronos.com",
                    new AddressRequest("25930790", "123")
            );
            companyUseCase.createCompany(companyRequest);
            log.info("Empresa 'Kronos Tech Solutions' criada com sucesso.");

            // 2. Crie um novo funcionário com a função de MANAGER
            var employeeRequest = new CreateEmployeeRequest(
                    "Gerente Principal",
                    "11111111111",
                    "Gerente de TI",
                    "gerente@kronos.com",
                    10000.00,
                    "11999999999",
                    new AddressRequest("25930790", "456")
            );
            employeeUseCase.createEmployee(employeeRequest);
            log.info("Funcionário 'Gerente Principal' criado com sucesso.");

            // 3. Crie um usuário com a função de MANAGER para o funcionário
            // Você precisará obter o ID do funcionário recém-criado para associar ao usuário
            // A implementação atual do EmployeeService não retorna o ID, então esta parte é uma simulação.
            // Em uma implementação real, o método `createEmployee` deveria retornar o ID ou haver um método de busca.
            // Para este exemplo, estamos usando um ID fictício.
            UUID fakeEmployeeId = UUID.randomUUID(); // SUBSTITUA ISSO PELO ID REAL DO FUNCIONÁRIO

            var userRequest = new CreateUserRequest(
                    "manager.user",
                    "Tec2659*",
                    Role.MANAGER.name(),
                    fakeEmployeeId
            );
            userUseCase.createUser(userRequest);
            log.info("Usuário 'manager.user' com a função de MANAGER criado com sucesso.");

        } catch (Exception e) {
            log.error("Erro ao criar dados iniciais: {}", e.getMessage());
        }
    }
}