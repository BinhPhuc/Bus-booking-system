package com.example.busbooking.config;

import com.example.busbooking.business.command.RegisterCommand;
import com.example.busbooking.business.service.BootstrapSuperAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class BootstrapSuperAdminConfigTest {
    private final UserConfig config = new UserConfig();
    private final BootstrapSuperAdminService service = mock(BootstrapSuperAdminService.class);

    @Test
    void disabledBootstrapDoesNothingAndNeedsNoCredentials() throws Exception {
        var props = new BootstrapSuperAdminProperties();
        config.superAdminBootstrapRunner(props, service).run(new DefaultApplicationArguments());
        verifyNoInteractions(service);
    }

    @Test
    void enabledBootstrapRunsWithoutAnyProfileAndPropertiesNeverPrintPassword() throws Exception {
        var props = new BootstrapSuperAdminProperties(); props.setEnabled(true);
        props.setEmail("root@example.com"); props.setFullName("Root"); props.setPhone("0901234567"); props.setPassword("SecretPassword123");
        when(service.bootstrap(any())).thenReturn(BootstrapSuperAdminService.Result.CREATED_EMAIL_PENDING);
        config.superAdminBootstrapRunner(props, service).run(new DefaultApplicationArguments());
        verify(service).bootstrap(new RegisterCommand("Root", "root@example.com", "SecretPassword123", "0901234567"));
        assertThat(props.toString()).doesNotContain("SecretPassword123");
    }
}
