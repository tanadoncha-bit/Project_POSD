package com.example.itborrow;

import com.example.itborrow.domain.entity.*;
import com.example.itborrow.domain.enums.*;
import com.example.itborrow.dto.request.*;
import com.example.itborrow.repository.*;
import com.example.itborrow.service.*;
import com.example.itborrow.config.LegacyPasswordUpgrade;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={
    "spring.datasource.url=jdbc:h2:mem:workflow;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=none", "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.flyway.enabled=false", "management.health.mail.enabled=false", "app.jobs.enabled=false", "app.mail.from=tests@example.test", "spring.mail.host=smtp.example.test", "spring.sql.init.mode=always", "spring.jpa.show-sql=false", "logging.level.org.hibernate.SQL=warn",
    "borrow.overdue.initial-delay-ms=86400000", "logging.level.root=WARN", "logging.level.org.springframework=WARN", "debug=false"
})
@AutoConfigureMockMvc
class WorkflowIntegrationTest {
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.example.itborrow.service.avatar.ImageStorage imageStorage;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    org.springframework.mail.javamail.JavaMailSender mailSender;
    @Autowired jakarta.persistence.EntityManagerFactory entityManagerFactory;
    @Autowired PersistentJobs jobs;
    @Autowired AvatarService avatarService;
    @Autowired MockMvc mvc;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired UserManagementService management;
    @Autowired UserRepository users;
    @Autowired UserProfileRepository profiles;
    @Autowired EquipmentRepository equipment;
    @Autowired EquipmentCategoryRepository categories;
    @Autowired BorrowRequestRepository requests;
    @Autowired ReturnRecordRepository returns;
    @Autowired BorrowRequestService borrowing;
    @Autowired AccountService accounts;
    @Autowired PasswordEncoder encoder;
    @Autowired PlatformTransactionManager transactions;
    @Autowired LegacyPasswordUpgrade upgrade;
    User alice, bob, admin;
    Equipment asset;

    @BeforeEach void seed() {
        SecurityContextHolder.clearContext();
        org.mockito.Mockito.when(imageStorage.readUrl(org.mockito.ArgumentMatchers.anyString()))
            .thenAnswer(call -> "https://storage.example.test/"+call.getArgument(0));
        jdbc.update("DELETE FROM role_audit");
        jdbc.update("DELETE FROM borrow_workflow_audit");
        jdbc.update("DELETE FROM settlements"); jdbc.update("DELETE FROM equipment_repairs"); jdbc.update("DELETE FROM delivery_jobs");
        returns.deleteAll(); requests.deleteAll(); equipment.deleteAll(); profiles.deleteAll(); users.deleteAll(); categories.deleteAll();
        alice=account("alice",Role.USER); bob=account("bob",Role.USER); admin=account("admin",Role.ADMIN);
        var category=categories.save(new EquipmentCategory("Laptop","Test"));
        asset=new Equipment("TEST-001","Test laptop",EquipmentStatus.AVAILABLE); asset.setCategoryId(category.getId()); asset.setPurchasePrice(new java.math.BigDecimal("1000.00")); equipment.save(asset);
    }
    @AfterEach void clearAuth() { SecurityContextHolder.clearContext(); }
    User account(String name, Role role) {
        var u=new User(name,name+"@example.test",role);u.setPassword(encoder.encode("Password123!")); return users.save(u);
    }
    BorrowRequest loan(User owner, BorrowStatus status, Equipment asset) {
        var b=new BorrowRequest();b.setUser(owner);b.setBorrowDate(LocalDate.now());b.setDueDate(LocalDate.now().plusDays(3));b.setStatus(status);
        var item=new BorrowItem();item.setEquipment(asset);item.setQuantity(1);b.addItem(item);return requests.save(b);
    }
    void auth(String name) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(name,"",List.of()));
    }
    String borrowJson(long claimedUser, String dueDate) {
        return "{\"userId\":"+claimedUser+",\"borrowDate\":\""+LocalDate.now()+"\",\"dueDate\":\""+dueDate+"\",\"items\":[{\"equipmentId\":"+asset.getId()+",\"quantity\":1}]}";
    }
    @Test void partialReturnsKeepFrozenFeesAndSettlementIsSeparate() throws Exception {
        var second=new Equipment("SECOND","Second",EquipmentStatus.AVAILABLE);second.setCategoryId(asset.getCategoryId());second.setPurchasePrice(new java.math.BigDecimal("1000"));equipment.save(second);
        auth("alice"); var dto=new BorrowRequestDto();dto.setBorrowDate(LocalDate.now());dto.setDueDate(LocalDate.now().plusDays(1));dto.setItems(List.of(new BorrowItemRequestDto(asset.getId(),1),new BorrowItemRequestDto(second.getId(),1)));
        long id=borrowing.createBorrowRequest(dto).getId(); auth("admin"); borrowing.approveBorrowRequest(id); auth("alice"); borrowing.pickUpEquipment(id);
        var request=requests.findById(id).orElseThrow();request.setBorrowDate(LocalDate.now().minusDays(4));request.setDueDate(LocalDate.now().minusDays(2));requests.save(request);
        alice.setRole(Role.VIP);users.save(alice);var changed=equipment.findById(asset.getId()).orElseThrow();changed.setPurchasePrice(new java.math.BigDecimal("9000"));equipment.save(changed);
        String partial="{\"partial\":true,\"items\":[{\"equipmentId\":"+asset.getId()+",\"condition\":\"SCRATCH\",\"remark\":\"Scratch\"}]}";
        mvc.perform(post("/api/v1/borrow-requests/"+id+"/return").with(user("admin").roles("ADMIN")).with(csrf()).contentType("application/json").content(partial))
            .andExpect(status().isOk()).andExpect(jsonPath("$.fineAmount").value(100)).andExpect(jsonPath("$.damageAmount").value(200));
        assertThat(requests.findById(id).orElseThrow().getStatus()).isEqualTo(BorrowStatus.BORROWED);
        mvc.perform(post("/api/v1/borrow-requests/"+id+"/return").with(user("admin").roles("ADMIN")).with(csrf()).contentType("application/json").content(partial)).andExpect(status().isBadRequest());
        String pay="{\"reference\":\"Receipt-1\",\"amount\":300}";
        mvc.perform(post("/api/v1/borrow-requests/"+id+"/settlement").with(user("admin").roles("ADMIN")).with(csrf()).contentType("application/json").content(pay)).andExpect(status().isConflict());
        mvc.perform(post("/api/v1/borrow-requests/"+id+"/return").with(user("admin").roles("ADMIN")).with(csrf()).contentType("application/json").content("{\"condition\":\"NORMAL\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalAmount").value(300));
        mvc.perform(get("/api/v1/borrow-requests/"+id+"/settlement").with(user("alice"))).andExpect(jsonPath("$.paid").value(false));
        mvc.perform(post("/api/v1/borrow-requests/"+id+"/settlement").with(user("alice")).with(csrf()).contentType("application/json").content(pay)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/borrow-requests/"+id+"/settlement").with(user("admin").roles("ADMIN")).with(csrf()).contentType("application/json").content(pay)).andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/borrow-requests/"+id+"/settlement").with(user("admin").roles("ADMIN")).with(csrf()).contentType("application/json").content(pay)).andExpect(status().isConflict());
    }
    @Test void rejectionAndRepairRequireReasonsAndOperators() throws Exception {
        var request=loan(alice,BorrowStatus.PENDING,asset);
        String url="/api/v1/borrow-requests/"+request.getId()+"/reject";
        mvc.perform(post(url).with(user("alice")).with(csrf()).contentType("application/json").content("{\"reason\":\"No\"}")).andExpect(status().isForbidden());
        mvc.perform(post(url).with(user("admin").roles("ADMIN")).with(csrf()).contentType("application/json").content("{\"reason\":\"Reserved for class\"}")).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/borrow-requests/"+request.getId()).with(user("alice"))).andExpect(jsonPath("$.rejectionReason").value("Reserved for class"));
        asset.setStatus(EquipmentStatus.MAINTENANCE);equipment.save(asset);
        mvc.perform(post("/api/v1/equipment/"+asset.getId()+"/repair").with(user("admin").roles("ADMIN")).with(csrf()).contentType("application/json").content("{\"reason\":\"Replaced keyboard and tested\"}")).andExpect(status().isNoContent());
        assertThat(equipment.findById(asset.getId()).orElseThrow().getStatus()).isEqualTo(EquipmentStatus.AVAILABLE);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM equipment_repairs",Long.class)).isEqualTo(1);
    }
    @Test void deliveryRetriesFailuresAndClearsSentPayload() {
        jobs.email("alice@example.test","Test","Private test body");
        org.mockito.Mockito.doThrow(new org.springframework.mail.MailSendException("Unavailable"))
            .doNothing().when(mailSender).send(org.mockito.ArgumentMatchers.any(org.springframework.mail.SimpleMailMessage.class));
        jobs.process();
        assertThat(jdbc.queryForObject("SELECT attempts FROM delivery_jobs",Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT completed FROM delivery_jobs",Boolean.class)).isFalse();
        jdbc.update("UPDATE delivery_jobs SET next_attempt=?",java.sql.Timestamp.from(java.time.Instant.now().minusSeconds(1)));
        jobs.process();
        assertThat(jdbc.queryForObject("SELECT completed FROM delivery_jobs",Boolean.class)).isTrue();
        assertThat(jdbc.queryForObject("SELECT payload FROM delivery_jobs",String.class)).isEmpty();
    }
    @Test void emailVerificationIsSingleUseAndBoundToCurrentEmail() throws Exception {
        mvc.perform(post("/api/v1/profile/verification").with(user("alice")).with(csrf())).andExpect(status().isNoContent());
        var body=jdbc.queryForObject("SELECT payload FROM delivery_jobs WHERE recipient=?",String.class,alice.getEmail());
        var token=body.substring(body.lastIndexOf(' ')+1);
        mvc.perform(post("/api/v1/profile/verification").with(user("alice")).with(csrf())).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/profile/verification/confirm").with(user("bob")).with(csrf()).contentType("application/json").content("{\"token\":\""+token+"\"}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/profile/verification/confirm").with(user("alice")).with(csrf()).contentType("application/json").content("{\"token\":\""+token+"\"}")).andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/profile/verification/confirm").with(user("alice")).with(csrf()).contentType("application/json").content("{\"token\":\""+token+"\"}")).andExpect(status().isBadRequest());
        alice.setEmail("new@example.test");users.save(alice);
        mvc.perform(get("/api/v1/profile/verification").with(user("alice"))).andExpect(jsonPath("$.verified").value(false));
        org.mockito.Mockito.verifyNoInteractions(mailSender);
    }
    @Test void paginatedInventoryFiltersOnServerAndMeasuresQueries() throws Exception {
        for(int i=0;i<40;i++) {var e=new Equipment("PERF-"+i,"Device "+i,EquipmentStatus.MAINTENANCE);e.setCategoryId(asset.getCategoryId());equipment.save(e);}
        var stats=entityManagerFactory.unwrap(org.hibernate.SessionFactory.class).getStatistics();stats.setStatisticsEnabled(true);stats.clear();long start=System.nanoTime();
        mvc.perform(get("/api/v1/equipment").param("size","12").param("status","MAINTENANCE").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(12)).andExpect(jsonPath("$.totalElements").value(40));
        long queries=stats.getPrepareStatementCount();stats.setStatisticsEnabled(false);
        assertThat(queries).isLessThan(10);
        java.nio.file.Files.writeString(java.nio.file.Path.of("target/performance-sample.txt"),"H2 authenticated inventory API: "+((System.nanoTime()-start)/1000000)+" ms, "+queries+" SQL statements, 12/40 assets. Not a browser or production benchmark.");
    }
    @Test void registrationIgnoresRoleAndCreatesProfileWithHashedPassword() throws Exception {
        mvc.perform(post("/api/v1/users").with(csrf()).contentType("application/json").content("""
            {"username":"newuser","email":"new@example.test","password":"Password123!","confirmPassword":"Password123!","fullName":"New User","phone":"123","department":"IT","role":"ADMIN","id":1}
            """)).andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("USER")).andExpect(jsonPath("$.password").doesNotExist());
        var u=users.findByUsername("newuser").orElseThrow();
        assertThat(encoder.matches("Password123!",u.getPassword())).isTrue();
        assertThat(profiles.findByUserId(u.getId()).orElseThrow().getFullName()).isEqualTo("New User");
        mvc.perform(post("/login").with(csrf()).param("username","newuser").param("password","Password123!"))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/"));
    }
    @Test void webRegistrationAndProfileUpdateWork() throws Exception {
        mvc.perform(post("/register").with(csrf()).param("username","webuser").param("email","web@example.test")
            .param("password","Password123!").param("confirmPassword","Password123!").param("fullName","Web User").param("phone","").param("department","IT"))
            .andExpect(redirectedUrl("/"));
        assertThat(users.findByUsername("webuser")).isPresent();
        mvc.perform(post("/profile").with(user("alice")).with(csrf()).param("email","changed@example.test").param("fullName","Alice Updated").param("phone","456").param("department","IT").param("role","ADMIN"))
            .andExpect(redirectedUrl("/profile"));
        assertThat(users.findById(alice.getId()).orElseThrow().getRole()).isEqualTo(Role.USER);
        assertThat(profiles.findByUserId(alice.getId()).orElseThrow().getFullName()).isEqualTo("Alice Updated");
        mvc.perform(get("/profile").with(user("alice"))).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Alice Updated")));
    }
    @Test void passwordChangeRequiresOldPassword() throws Exception {
        mvc.perform(post("/profile/change-password").with(user("alice")).with(csrf()).param("currentPassword","bad").param("password","NewPassword123!").param("confirmPassword","NewPassword123!"))
            .andExpect(redirectedUrl("/profile/change-password"));
        assertThat(encoder.matches("Password123!",users.findById(alice.getId()).orElseThrow().getPassword())).isTrue();
        mvc.perform(post("/profile/change-password").with(user("alice")).with(csrf()).param("currentPassword","Password123!").param("password","NewPassword123!").param("confirmPassword","NewPassword123!"))
            .andExpect(redirectedUrl("/profile"));
        assertThat(encoder.matches("NewPassword123!",users.findById(alice.getId()).orElseThrow().getPassword())).isTrue();
    }
    @Test void borrowedIdentityComesFromLoginAndListIsPrivate() throws Exception {
        loan(bob,BorrowStatus.PENDING,asset);
        mvc.perform(post("/api/v1/borrow-requests").with(user("alice")).with(csrf()).contentType("application/json").content(borrowJson(bob.getId(),LocalDate.now().plusDays(2).toString())))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.userId").value(alice.getId()));
        mvc.perform(get("/api/v1/borrow-requests").with(user("alice"))).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].username").value("alice"));
        mvc.perform(get("/api/v1/borrow-requests")).andExpect(status().isUnauthorized());
    }
    @Test void ownersCannotApproveAndOtherUsersCannotReadOrCancel() throws Exception {
        var b=loan(alice,BorrowStatus.PENDING,asset);
        mvc.perform(patch("/api/v1/borrow-requests/"+b.getId()+"/approve").with(user("alice")).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/borrow-requests/"+b.getId()).with(user("bob"))).andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/borrow-requests/"+b.getId()+"/cancel").with(user("bob")).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/borrow-requests/"+b.getId()+"/cancel").with(user("alice")).with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
    }
    @Test void operatorsCannotApproveTheirOwnRequests() throws Exception {
        var operator = users.findByUsername("admin").orElseThrow();
        for (var role : List.of(Role.ADMIN, Role.STAFF)) {
            operator.setRole(role); users.saveAndFlush(operator);
            var own = loan(operator, BorrowStatus.PENDING, asset);
            mvc.perform(patch("/api/v1/borrow-requests/" + own.getId() + "/approve")
                .with(user("admin").roles(role.name())).with(csrf()))
                .andExpect(status().isForbidden());
            assertThat(requests.findById(own.getId()).orElseThrow().getStatus()).isEqualTo(BorrowStatus.PENDING);
        }
    }
    @Test void borrowersConfirmTheirOwnPickupButCannotInspectTheirOwnReturn() throws Exception {
        var own=loan(admin,BorrowStatus.APPROVED,asset);
        mvc.perform(patch("/api/v1/borrow-requests/"+own.getId()+"/pickup").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isOk());
        own.setStatus(BorrowStatus.BORROWED); requests.save(own);
        mvc.perform(post("/api/v1/borrow-requests/"+own.getId()+"/return").with(user("admin").roles("ADMIN")).with(csrf()).contentType("application/json").content("{\"condition\":\"NORMAL\"}"))
            .andExpect(status().isForbidden());
        assertThat(requests.findById(own.getId()).orElseThrow().getStatus()).isEqualTo(BorrowStatus.BORROWED);
    }
    @Test void overlappingApprovalIsRejectedAndSlotReachesHistory() throws Exception {
        asset.setStorageSlot("A-4"); asset.setImageUrl("/images/test-equipment.png"); equipment.save(asset);
        var first=loan(alice,BorrowStatus.PENDING,asset);
        var second=loan(bob,BorrowStatus.PENDING,asset);
        mvc.perform(patch("/api/v1/borrow-requests/"+first.getId()+"/approve").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isOk());
        mvc.perform(patch("/api/v1/borrow-requests/"+second.getId()+"/approve").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isConflict());
        assertThat(requests.findById(second.getId()).orElseThrow().getStatus()).isEqualTo(BorrowStatus.PENDING);
        mvc.perform(get("/my-requests").with(user("alice")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("A-4")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("src=\"/images/test-equipment.png\"")));
        mvc.perform(get("/api/v1/borrow-requests/"+first.getId()).with(user("alice")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].imageUrl").value("/images/test-equipment.png"));
    }
    @Test void staleMetadataCannotReplaceStorageManagedImage() throws Exception {
        asset.setImageUrl("/images/equipment/"+asset.getId()+"/abc.png"); equipment.save(asset);
        mvc.perform(put("/api/v1/equipment/"+asset.getId()).with(user("admin").roles("ADMIN")).with(csrf()).contentType("application/json")
            .content("{\"name\":\"Updated name\",\"status\":\"AVAILABLE\",\"imageUrl\":\"/images/equipment/1/old.png\"}"))
            .andExpect(status().isOk());
        assertThat(equipment.findById(asset.getId()).orElseThrow().getImageUrl()).isEqualTo(asset.getImageUrl());
    }
    @Test void simulatedLockerIsPrivateSingleUseAndRevokedAtPickup() throws Exception {
        var request=loan(alice,BorrowStatus.PENDING,asset);
        mvc.perform(patch("/api/v1/borrow-requests/"+request.getId()+"/approve").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isOk());
        String url="/api/v1/borrow-requests/"+request.getId()+"/locker";
        mvc.perform(get(url).with(user("bob"))).andExpect(status().isForbidden());
        mvc.perform(get(url).with(user("alice"))).andExpect(status().isOk()).andExpect(jsonPath("$.simulation").value(true)).andExpect(header().string("Cache-Control",org.hamcrest.Matchers.containsString("no-store")));
        String pin=jdbc.queryForObject("SELECT pin FROM locker_access WHERE request_id=?",String.class,request.getId());
        assertThat(pin).matches("[0-9]{6}");
        mvc.perform(post(url).with(user("alice")).with(csrf()).contentType("application/json").content("{\"pin\":\"bad\"}")).andExpect(status().isBadRequest());
        mvc.perform(post(url).with(user("alice")).with(csrf()).contentType("application/json").content("{\"pin\":\""+pin+"\"}")).andExpect(status().isNoContent());
        mvc.perform(post(url).with(user("alice")).with(csrf()).contentType("application/json").content("{\"pin\":\""+pin+"\"}")).andExpect(status().isBadRequest());
        mvc.perform(patch("/api/v1/borrow-requests/"+request.getId()+"/pickup").with(user("alice")).with(csrf())).andExpect(status().isOk());
        mvc.perform(get(url).with(user("alice"))).andExpect(status().isBadRequest());
        assertThat(equipment.findById(asset.getId()).orElseThrow().getStatus()).isEqualTo(EquipmentStatus.IN_USE);
    }

    @Test void deletionEligibilityAndUserSearch() throws Exception {
        mvc.perform(get("/api/v1/equipment/"+asset.getId()+"/deletion").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.allowed").value(true));
        loan(alice,BorrowStatus.CANCELLED,asset);
        mvc.perform(get("/api/v1/equipment/"+asset.getId()+"/deletion").with(user("admin").roles("ADMIN")))
            .andExpect(jsonPath("$.allowed").value(false));
        mvc.perform(get("/admin/users").param("keyword","alice").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk()).andExpect(model().attribute("accounts",org.hamcrest.Matchers.hasProperty("totalElements",org.hamcrest.Matchers.is(1L))));
    }

    @Test void userNavigationQueryBudget() throws Exception {
        for (int i=0;i<12;i++) loan(alice,BorrowStatus.PENDING,asset);
        var stats=entityManagerFactory.unwrap(org.hibernate.SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
        var measurements=new StringBuilder();
        for (String path : List.of("/", "/equipment", "/my-requests")) {
            stats.clear();
            long start=System.nanoTime();
            mvc.perform(get(path).with(user("alice"))).andExpect(status().isOk());
            assertThat(stats.getPrepareStatementCount()).as("Hibernate query budget for %s",path)
                .isBetween(1L,path.equals("/my-requests") ? 8L : 5L);
            measurements.append(path).append(": ").append(stats.getPrepareStatementCount()).append(" SQL, ")
                .append((System.nanoTime()-start)/1000000).append(" ms\n");
        }
        java.nio.file.Files.writeString(java.nio.file.Path.of("target/user-navigation-performance.txt"), measurements.toString());
        stats.setStatisticsEnabled(false);
    }

    @Test void userPagesAreBoundedAndFiltersApplyBeforePagination() throws Exception {
        for(int i=0;i<14;i++) {
            var item=new Equipment("PAGE-"+i,"Page device "+i,EquipmentStatus.AVAILABLE);
            item.setCategoryId(asset.getCategoryId()); item.setPurchasePrice(new java.math.BigDecimal("100")); equipment.save(item);
        }
        for(int i=0;i<12;i++) loan(alice,BorrowStatus.PENDING,asset);
        loan(alice,BorrowStatus.RETURNED,asset); loan(bob,BorrowStatus.RETURNED,asset);
        mvc.perform(get("/equipment")).andExpect(status().isOk()).andExpect(model().attribute("equipments",org.hamcrest.Matchers.hasSize(12)));
        mvc.perform(get("/equipment?page=1")).andExpect(model().attribute("equipments",org.hamcrest.Matchers.hasSize(3)));
        mvc.perform(get("/equipment?keyword=PAGE-13")).andExpect(model().attribute("equipments",org.hamcrest.Matchers.hasSize(1)));
        mvc.perform(get("/my-requests").with(user("alice"))).andExpect(model().attribute("requests",org.hamcrest.Matchers.hasSize(10)));
        mvc.perform(get("/my-requests?status=RETURNED").with(user("alice"))).andExpect(model().attribute("requests",org.hamcrest.Matchers.hasSize(1)));
        mvc.perform(get("/my-requests?status=PENDING&page=1").with(user("alice")))
            .andExpect(model().attribute("requests",org.hamcrest.Matchers.hasSize(2)))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Please log in to view your request history."))))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("data-request-filter=\"PENDING\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("class=\"request-filter-count\">12</span>")));
        mvc.perform(get("/borrow?modal=true&equipmentIds="+asset.getId()).with(user("alice"))).andExpect(model().attribute("availableEquipments",org.hamcrest.Matchers.hasSize(1)));
    }
    @Test void profilePaginatesActiveAndPastLoansIndependently() throws Exception {
        for(int i=0;i<7;i++) { loan(alice,BorrowStatus.BORROWED,asset); loan(alice,BorrowStatus.RETURNED,asset); }
        mvc.perform(get("/profile").with(user("alice"))).andExpect(status().isOk())
            .andExpect(model().attribute("activeLoans",org.hamcrest.Matchers.hasSize(5)))
            .andExpect(model().attribute("borrowHistory",org.hamcrest.Matchers.hasSize(5)))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("historyPage=1")));
        mvc.perform(get("/profile?activePage=1&historyPage=1").with(user("alice")))
            .andExpect(model().attribute("activeLoans",org.hamcrest.Matchers.hasSize(2)))
            .andExpect(model().attribute("borrowHistory",org.hamcrest.Matchers.hasSize(2)));
    }
    @Test void successfulWorkflowIsAuditedAndExpiredRequestsLeaveTheQueue() throws Exception {
        var request=loan(alice,BorrowStatus.PENDING,asset);
        mvc.perform(patch("/api/v1/borrow-requests/"+request.getId()+"/approve").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT actor_username FROM borrow_workflow_audit WHERE request_id=? AND action='APPROVED'",String.class,request.getId())).isEqualTo("admin");
        mvc.perform(patch("/api/v1/borrow-requests/"+request.getId()+"/approve").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM borrow_workflow_audit WHERE request_id=?",Long.class,request.getId())).isEqualTo(1L);
        request=requests.findById(request.getId()).orElseThrow(); request.setDueDate(LocalDate.now().minusDays(1)); requests.save(request);
        SecurityContextHolder.clearContext(); borrowing.checkAndMarkOverdue();
        assertThat(requests.findById(request.getId()).orElseThrow().getStatus()).isEqualTo(BorrowStatus.CANCELLED);
        assertThat(jdbc.queryForObject("SELECT actor_username FROM borrow_workflow_audit WHERE request_id=? AND action='EXPIRED'",String.class,request.getId())).isEqualTo("SYSTEM");
    }
    @Test void managementRequestPagesAreBoundedFilteredAndOperatorOnly() throws Exception {
        for(int i=0;i<22;i++) loan(alice,BorrowStatus.PENDING,asset);
        mvc.perform(get("/api/v1/borrow-requests/management").with(user("alice"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/borrow-requests/management").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.page.content.length()").value(20)).andExpect(jsonPath("$.pending").value(22));
        mvc.perform(get("/api/v1/borrow-requests/management?page=1").with(user("admin").roles("ADMIN")))
            .andExpect(jsonPath("$.page.content.length()").value(2));
        mvc.perform(get("/api/v1/borrow-requests/management?status=RETURNED").with(user("admin").roles("ADMIN")))
            .andExpect(jsonPath("$.page.totalElements").value(0)).andExpect(jsonPath("$.pending").value(22));
    }
    @Test void operatorActionsRequireCsrfAndDatabaseOperatorRole() throws Exception {
        var b=loan(alice,BorrowStatus.PENDING,asset);
        mvc.perform(patch("/api/v1/borrow-requests/"+b.getId()+"/approve").with(user("admin").roles("ADMIN"))).andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/borrow-requests/"+b.getId()+"/approve").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));
        auth("bob");assertThatThrownBy(() -> borrowing.pickUpEquipment(b.getId())).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }
    @Test void pickupRequiresOwnerCsrfAndBorrowingPeriodAndRevokesPin() throws Exception {
        var request=loan(alice,BorrowStatus.PENDING,asset);
        mvc.perform(patch("/api/v1/borrow-requests/"+request.getId()+"/approve").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isOk());
        String url="/api/v1/borrow-requests/"+request.getId()+"/pickup";
        mvc.perform(get("/my-requests").with(user("alice"))).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("data-locker-pin")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("data-confirm-pickup")))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("View locker PIN"))));
        mvc.perform(patch(url).with(user("bob")).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(patch(url).with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(patch(url).with(user("alice"))).andExpect(status().isForbidden());
        request=requests.findById(request.getId()).orElseThrow();
        request.setBorrowDate(LocalDate.now().plusDays(1)); request.setDueDate(LocalDate.now().plusDays(2)); requests.save(request);
        mvc.perform(patch(url).with(user("alice")).with(csrf())).andExpect(status().isBadRequest());
        request.setBorrowDate(LocalDate.now().minusDays(2)); request.setDueDate(LocalDate.now().minusDays(1)); requests.save(request);
        mvc.perform(patch(url).with(user("alice")).with(csrf())).andExpect(status().isBadRequest());
        request.setBorrowDate(LocalDate.now()); request.setDueDate(LocalDate.now().plusDays(1)); requests.save(request);
        mvc.perform(patch(url).with(user("alice")).with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("BORROWED"));
        assertThat(jdbc.queryForObject("SELECT pin FROM locker_access WHERE request_id=?",String.class,request.getId())).isNull();
        mvc.perform(patch(url).with(user("alice")).with(csrf())).andExpect(status().isConflict());
    }
    @Test void parallelPickupsHaveExactlyOneWinner() throws Exception {
        var first=loan(alice,BorrowStatus.APPROVED,asset);var second=loan(bob,BorrowStatus.APPROVED,asset);
        var pool=Executors.newFixedThreadPool(2);var gate=new CountDownLatch(1);
        try {
            var one=pool.submit(() -> pickup(first.getId(),"alice",gate));var two=pool.submit(() -> pickup(second.getId(),"bob",gate));gate.countDown();
            assertThat(List.of(one.get(15,TimeUnit.SECONDS),two.get(15,TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);
            assertThat(requests.findAll().stream().filter(b -> b.getStatus()==BorrowStatus.BORROWED).count()).isEqualTo(1);
            assertThat(equipment.findById(asset.getId()).orElseThrow().getStatus()).isEqualTo(EquipmentStatus.IN_USE);
        } finally { pool.shutdownNow(); }
    }
    boolean pickup(Long id, String username, CountDownLatch gate) throws Exception {
        gate.await();auth(username);
        try { borrowing.pickUpEquipment(id); return true; }
        catch (com.example.itborrow.exception.EquipmentNotAvailableException ex) { return false; }
        finally { SecurityContextHolder.clearContext(); }
    }
    @Test void pickupRollsBackAllAssetsWhenOneIsUnavailable() {
        var b=loan(alice,BorrowStatus.APPROVED,asset);
        var blocked=new Equipment("BLOCKED","Blocked",EquipmentStatus.IN_USE);blocked.setCategoryId(asset.getCategoryId());equipment.save(blocked);
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {var loaded=requests.findById(b.getId()).orElseThrow();var item=new BorrowItem();item.setEquipment(blocked);loaded.addItem(item);requests.save(loaded);});
        auth("alice");assertThatThrownBy(() -> borrowing.pickUpEquipment(b.getId())).isInstanceOf(com.example.itborrow.exception.EquipmentNotAvailableException.class);
        assertThat(equipment.findById(asset.getId()).orElseThrow().getStatus()).isEqualTo(EquipmentStatus.AVAILABLE);
        assertThat(requests.findById(b.getId()).orElseThrow().getStatus()).isEqualTo(BorrowStatus.APPROVED);
    }
    @Test void returnRejectsBackdateAndDamagedAssetIsNotAvailable() throws Exception {
        asset.setStatus(EquipmentStatus.IN_USE);equipment.save(asset);var b=loan(alice,BorrowStatus.BORROWED,asset);
        b.setBorrowDate(LocalDate.now().minusDays(4));b.setDueDate(LocalDate.now().minusDays(2));requests.save(b);
        String url="/api/v1/borrow-requests/"+b.getId()+"/return";
        mvc.perform(post(url).with(user("admin").roles("ADMIN")).with(csrf()).contentType("application/json").content("{\"condition\":\"GOOD\",\"returnDate\":\""+LocalDate.now().minusDays(2)+"\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post(url).with(user("alice")).with(csrf()).contentType("application/json").content("{\"condition\":\"GOOD\"}" )).andExpect(status().isForbidden());
        mvc.perform(post(url).with(user("admin").roles("ADMIN")).with(csrf()).contentType("application/json").content("{\"condition\":\"DAMAGED\",\"remark\":\"Broken screen\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.fineAmount").value(100)).andExpect(jsonPath("$.damageAmount").value(500)).andExpect(jsonPath("$.totalAmount").value(600)).andExpect(jsonPath("$.returnDate").value(LocalDate.now().toString()));
        assertThat(equipment.findById(asset.getId()).orElseThrow().getStatus()).isEqualTo(EquipmentStatus.MAINTENANCE);
        mvc.perform(post(url).with(user("admin").roles("ADMIN")).with(csrf()).contentType("application/json").content("{\"condition\":\"GOOD\"}" )).andExpect(status().isConflict());
        mvc.perform(get(url).with(user("bob"))).andExpect(status().isForbidden());
    }
    @Test void invalidDatesAndDuplicateAssetsAreRejected() throws Exception {
        mvc.perform(post("/api/v1/borrow-requests").with(user("alice")).with(csrf()).contentType("application/json").content(borrowJson(alice.getId(),LocalDate.now().minusDays(1).toString()))).andExpect(status().isBadRequest());
        auth("alice");var dto=new BorrowRequestDto();dto.setBorrowDate(LocalDate.now());dto.setDueDate(LocalDate.now().plusDays(1));dto.setItems(List.of(new BorrowItemRequestDto(asset.getId(),1),new BorrowItemRequestDto(asset.getId(),1)));
        assertThatThrownBy(() -> borrowing.createBorrowRequest(dto)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void overdueJobExpiresUncollectedRequestsAndIsRepeatable() {
        var b=loan(alice,BorrowStatus.BORROWED,asset);b.setDueDate(LocalDate.now().minusDays(1));requests.save(b);
        var pending=loan(bob,BorrowStatus.PENDING,asset);pending.setDueDate(LocalDate.now().minusDays(1));requests.save(pending);
        borrowing.checkAndMarkOverdue();borrowing.checkAndMarkOverdue();
        assertThat(requests.findById(b.getId()).orElseThrow().getStatus()).isEqualTo(BorrowStatus.OVERDUE);
        assertThat(requests.findById(pending.getId()).orElseThrow().getStatus()).isEqualTo(BorrowStatus.CANCELLED);
    }
    @Test void plaintextSeedUpgradeIsIdempotent() {
        alice.setPassword("LegacyPassword123!");users.save(alice);upgrade.run(null);
        String hash=users.findById(alice.getId()).orElseThrow().getPassword();assertThat(encoder.matches("LegacyPassword123!",hash)).isTrue();
        upgrade.run(null);assertThat(users.findById(alice.getId()).orElseThrow().getPassword()).isEqualTo(hash);
    }
    @Test void submittedEquipmentIdentitySurvivesInventoryEdits() throws Exception {
        asset.setStorageSlot("A-1"); asset.setImageUrl("/images/original.png"); equipment.saveAndFlush(asset);
        mvc.perform(post("/api/v1/borrow-requests").with(user("alice")).with(csrf()).contentType("application/json")
            .content(borrowJson(alice.getId(), LocalDate.now().plusDays(2).toString())))
            .andExpect(status().isCreated());
        var id = requests.findAll().get(0).getId();
        var accessory = categories.save(new EquipmentCategory("Accessory", "Test"));
        String update = """
            {"name":"Razer mouse","status":"AVAILABLE","categoryId":%d,"storageSlot":" c-2 ","imageUrl":"/images/new.png"}
            """.formatted(accessory.getId());
        mvc.perform(put("/api/v1/equipment/"+asset.getId()).with(user("admin").roles("ADMIN")).with(csrf())
            .contentType("application/json").content(update)).andExpect(status().isOk());
        assertThat(equipment.findById(asset.getId()).orElseThrow().getCategoryId()).isEqualTo(accessory.getId());
        assertThat(equipment.findById(asset.getId()).orElseThrow().getStorageSlot()).isEqualTo("C-2");
        mvc.perform(get("/api/v1/borrow-requests/"+id).with(user("alice")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].equipmentName").value("Test laptop"))
            .andExpect(jsonPath("$.items[0].categoryName").value("Laptop"))
            .andExpect(jsonPath("$.items[0].storageSlot").value("A-1"))
            .andExpect(jsonPath("$.items[0].imageUrl").value("/images/original.png"));
        mvc.perform(get("/my-requests").param("keyword", "Test laptop").with(user("alice")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("/images/original.png")));
        mvc.perform(put("/api/v1/equipment/"+asset.getId()).with(user("admin").roles("ADMIN")).with(csrf())
            .contentType("application/json").content("{\"name\":\"Invalid\",\"status\":\"AVAILABLE\",\"categoryId\":999999}"))
            .andExpect(status().isBadRequest());
    }
    @Test void slotsCannotBeSharedAndCanBeReleased() throws Exception {
        asset.setStorageSlot("C-2"); equipment.saveAndFlush(asset);
        String create = """
            {"name":"Mouse","assetCode":"NEW-SLOT","categoryId":%d,"purchasePrice":100,"storageSlot":" c-2 "}
            """.formatted(asset.getCategoryId());
        mvc.perform(post("/api/v1/equipment").with(user("admin").roles("ADMIN")).with(csrf())
            .contentType("application/json").content(create)).andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("slot is already assigned")));
        mvc.perform(put("/api/v1/equipment/"+asset.getId()).with(user("admin").roles("ADMIN")).with(csrf())
            .contentType("application/json").content("{\"name\":\"Test laptop\",\"status\":\"AVAILABLE\",\"storageSlot\":\"\"}"))
            .andExpect(status().isOk());
        assertThat(equipment.findById(asset.getId()).orElseThrow().getStorageSlot()).isNull();
        mvc.perform(post("/api/v1/equipment").with(user("admin").roles("ADMIN")).with(csrf())
            .contentType("application/json").content(create)).andExpect(status().isCreated());
        mvc.perform(put("/api/v1/equipment/"+asset.getId()).with(user("admin").roles("ADMIN")).with(csrf())
            .contentType("application/json").content("{\"name\":\"Test laptop\",\"status\":\"AVAILABLE\",\"storageSlot\":\"C-2\"}"))
            .andExpect(status().isConflict());
    }
    @Test void deletingEquipmentPreservesRequestHistoryAndExplainsWhy() throws Exception {
        var request = loan(alice, BorrowStatus.CANCELLED, asset);
        mvc.perform(delete("/api/v1/equipment/"+asset.getId()).with(user("admin").roles("ADMIN")).with(csrf()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("including cancelled or returned requests")));
        assertThat(equipment.existsById(asset.getId())).isTrue();
        assertThat(requests.existsById(request.getId())).isTrue();
        var unused = new Equipment();
        unused.setName("Unused asset"); unused.setAssetCode("UNUSED-DELETE");
        unused.setCategoryId(asset.getCategoryId()); unused.setStatus(EquipmentStatus.AVAILABLE);
        unused = equipment.saveAndFlush(unused);
        mvc.perform(delete("/api/v1/equipment/"+unused.getId()).with(user("admin").roles("ADMIN")).with(csrf()))
            .andExpect(status().is2xxSuccessful());
        assertThat(equipment.existsById(unused.getId())).isFalse();
    }
    @Test void equipmentCrudPreservesInUseInvariant() throws Exception {
        String json="{\"name\":\"New asset\",\"purchasePrice\":25000,\"assetCode\":\"NEW-001\",\"categoryId\":"+asset.getCategoryId()+"}";
        mvc.perform(post("/api/v1/equipment").with(user("admin").roles("ADMIN")).with(csrf()).contentType("application/json").content(json))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.categoryId").value(asset.getCategoryId()));
        mvc.perform(post("/api/v1/equipment").with(user("alice")).with(csrf()).contentType("application/json").content(json)).andExpect(status().isForbidden());
        asset.setStatus(EquipmentStatus.IN_USE);equipment.save(asset);
        mvc.perform(put("/api/v1/equipment/"+asset.getId()).with(user("admin").roles("ADMIN")).with(csrf()).contentType("application/json").content("{\"name\":\"Test laptop\",\"status\":\"AVAILABLE\"}"))
            .andExpect(status().isConflict());
        mvc.perform(delete("/api/v1/equipment/"+asset.getId()).with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isConflict());
    }
    @Test void lostReturnDisposesAssetAndInvalidConditionDoesNotChangeLoan() throws Exception {
        asset.setStatus(EquipmentStatus.IN_USE);equipment.save(asset);var b=loan(alice,BorrowStatus.BORROWED,asset);
        String url="/api/v1/borrow-requests/"+b.getId()+"/return";
        mvc.perform(post(url).with(user("admin").roles("ADMIN")).with(csrf()).contentType("application/json").content("{\"condition\":\"unknown\"}" )).andExpect(status().isBadRequest());
        assertThat(requests.findById(b.getId()).orElseThrow().getStatus()).isEqualTo(BorrowStatus.BORROWED);
        mvc.perform(post(url).with(user("admin").roles("ADMIN")).with(csrf()).contentType("application/json").content("{\"condition\":\"LOST\",\"remark\":\"Missing device\"}" )).andExpect(status().isOk()).andExpect(jsonPath("$.damageAmount").value(1000));
        assertThat(equipment.findById(asset.getId()).orElseThrow().getStatus()).isEqualTo(EquipmentStatus.DISPOSED);
    }
    @Test void pagesRenderWithActualBorrowHistory() throws Exception {
        var b=loan(alice,BorrowStatus.OVERDUE,asset);
        for (String url : List.of("/","/profile","/my-requests"))
            mvc.perform(get(url).with(user("alice"))).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("</html>")));
        mvc.perform(get("/my-requests").with(user("alice"))).andExpect(content().string(org.hamcrest.Matchers.containsString("overdue request(s)")));
    }
    @Test void borrowModalUsesAvailableDatabaseEquipment() throws Exception {
        asset.setStatus(EquipmentStatus.AVAILABLE); equipment.save(asset);
        mvc.perform(get("/borrow").param("modal", "true").with(user("alice")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"borrow-request-form\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString(asset.getName())))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<html"))));
        asset.setStatus(EquipmentStatus.MAINTENANCE); equipment.save(asset);
        mvc.perform(get("/borrow").param("modal", "true").with(user("alice")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(asset.getName()))));
    }
    @Test void equipmentSearchMatchesNameAndAssetCodeAndHandlesNoResults() throws Exception {
        mvc.perform(get("/api/v1/equipment").param("keyword", asset.getName().toLowerCase()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(asset.getId()));
        mvc.perform(get("/api/v1/equipment").param("keyword", asset.getAssetCode().toLowerCase()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(asset.getId()));
        mvc.perform(get("/api/v1/equipment").param("keyword", "no-matching-equipment-xyz"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
    }
    @Test void serverRenderedPagesCompleteWithNewSessions() throws Exception {
        for (String url : List.of("/","/equipment","/equipment/"+asset.getId()))
            mvc.perform(get(url)).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("</html>"))).andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
        for (String url : List.of("/profile","/borrow","/my-requests","/profile/change-password"))
            mvc.perform(get(url).with(user("alice"))).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("</html>")));
        mvc.perform(get("/admin").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
    }

    @Test void mixedReturnChargesOnlyDamagedItemsAndPreservesReceipt() throws Exception {
        asset.setStatus(EquipmentStatus.IN_USE); asset.setPurchasePrice(new java.math.BigDecimal("999.99")); equipment.save(asset);
        var second = new Equipment("SECOND", "Second laptop", EquipmentStatus.IN_USE);
        second.setCategoryId(asset.getCategoryId()); second.setPurchasePrice(new java.math.BigDecimal("5000")); equipment.save(second);
        var b = loan(alice, BorrowStatus.BORROWED, asset);
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            var loaded = requests.findById(b.getId()).orElseThrow();
            var item = new BorrowItem(); item.setEquipment(second); item.setQuantity(1); loaded.addItem(item); requests.save(loaded);
        });
        String url = "/api/v1/borrow-requests/" + b.getId() + "/return";
        String json = """
            {"items":[{"equipmentId":%d,"condition":"SCRATCH","remark":"New scratches on lid"},
                      {"equipmentId":%d,"condition":"NORMAL"}],"damageAmount":1,"fineAmount":1000}
            """.formatted(asset.getId(), second.getId());
        mvc.perform(post(url).with(user("admin").roles("ADMIN")).with(csrf()).contentType("application/json").content(json))
            .andExpect(status().isOk()).andExpect(jsonPath("$.condition").value("MIXED"))
            .andExpect(jsonPath("$.fineAmount").value(0)).andExpect(jsonPath("$.damageAmount").value(200))
            .andExpect(jsonPath("$.totalAmount").value(200)).andExpect(jsonPath("$.items.length()").value(2));
        asset.setPurchasePrice(new java.math.BigDecimal("2000")); equipment.save(asset);
        mvc.perform(get(url).with(user("alice"))).andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].purchasePrice").value(999.99)).andExpect(jsonPath("$.totalAmount").value(200));
        assertThat(equipment.findById(second.getId()).orElseThrow().getStatus()).isEqualTo(EquipmentStatus.AVAILABLE);
    }
    @Test void invalidInspectionsRollbackAndMissingPricesNeverBecomeFreeDamage() throws Exception {
        asset.setStatus(EquipmentStatus.IN_USE); asset.setPurchasePrice(null); equipment.save(asset);
        var b = loan(alice, BorrowStatus.BORROWED, asset);
        String url = "/api/v1/borrow-requests/" + b.getId() + "/return";
        for (String json : List.of(
            """
            { "items": [] }""",
            """
            { "items": [{"equipmentId":%d,"condition":"NORMAL"},{"equipmentId":%d,"condition":"NORMAL"}] }""".formatted(asset.getId(),asset.getId()),
            """
            { "items": [{"equipmentId":999999,"condition":"NORMAL"}] }""",
            """
            { "condition":"DAMAGED", "remark":"Broken screen" }""",
            """
            { "condition":"DAMAGED" }""")) {
            mvc.perform(post(url).with(user("admin").roles("ADMIN")).with(csrf()).contentType("application/json").content(json)).andExpect(status().isBadRequest());
            assertThat(requests.findById(b.getId()).orElseThrow().getStatus()).isEqualTo(BorrowStatus.BORROWED);
            assertThat(equipment.findById(asset.getId()).orElseThrow().getStatus()).isEqualTo(EquipmentStatus.IN_USE);
            assertThat(returns.findByBorrowRequestId(b.getId())).isEmpty();
        }
    }
    @Test void equipmentPriceRejectsNegativeAndFractionalCents() throws Exception {
        for (String price : List.of("-1", "12.345", "10000000000", "null")) {
            String json = """
            {"name":"Laptop","assetCode":"PRICE","categoryId":%d,"purchasePrice":%s}""".formatted(asset.getCategoryId(),price);
            mvc.perform(post("/api/v1/equipment").with(user("admin").roles("ADMIN")).with(csrf()).contentType("application/json").content(json)).andExpect(status().isBadRequest());
        }
    }
    @Test void registrationFailureStaysInModalWithoutFlashingPasswords() throws Exception {
        var result = mvc.perform(post("/register").with(csrf()).param("username","newuser").param("email","new@example.test")
            .param("password","Password123!").param("confirmPassword","Different123!").param("fullName","New User"))
            .andExpect(redirectedUrl("/")).andExpect(flash().attribute("registrationError","Passwords do not match.")).andReturn();
        assertThat(result.getFlashMap().toString()).doesNotContain("Password123!", "Different123!");
        mvc.perform(get("/").flashAttrs(result.getFlashMap())).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Passwords do not match.")));
    }

    @Test void laterInspectionFailureRollsBackEarlierItem() throws Exception {
        asset.setStatus(EquipmentStatus.IN_USE); equipment.save(asset);
        var second = new Equipment("ROLLBACK", "Unpriced laptop", EquipmentStatus.IN_USE);
        second.setCategoryId(asset.getCategoryId()); equipment.save(second);
        var b = loan(alice, BorrowStatus.BORROWED, asset);
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            var loaded = requests.findById(b.getId()).orElseThrow();
            var item = new BorrowItem(); item.setEquipment(second); item.setQuantity(1); loaded.addItem(item); requests.save(loaded);
        });
        String json = """
            {"items":[{"equipmentId":%d,"condition":"NORMAL"},{"equipmentId":%d,"condition":"DAMAGED","remark":"Broken"}]}
            """.formatted(asset.getId(),second.getId());
        mvc.perform(post("/api/v1/borrow-requests/"+b.getId()+"/return").with(user("admin").roles("ADMIN")).with(csrf()).contentType("application/json").content(json)).andExpect(status().isBadRequest());
        assertThat(equipment.findById(asset.getId()).orElseThrow().getStatus()).isEqualTo(EquipmentStatus.IN_USE);
        assertThat(requests.findById(b.getId()).orElseThrow().getStatus()).isEqualTo(BorrowStatus.BORROWED);
        assertThat(returns.findByBorrowRequestId(b.getId())).isEmpty();
    }
    @Test void managerUsesSharedLoginAndHasManagementLink() throws Exception {
        account("operator",Role.STAFF);
        var login = mvc.perform(post("/login").with(csrf()).param("username","operator").param("password","Password123!"))
            .andExpect(redirectedUrl("/")).andReturn();
        var session = (MockHttpSession) login.getRequest().getSession(false);
        mvc.perform(get("/").session(session)).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Management")));
        mvc.perform(get("/admin").session(session)).andExpect(status().isOk());
        mvc.perform(get("/admin").with(user("alice"))).andExpect(status().isForbidden());
    }
    @Test void additiveMigrationIsRepeatableAndPreservesData() {
        var populator = new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator(
            new org.springframework.core.io.FileSystemResource("doc/migrations/20260926_return_damage.sql"));
        var source = ((org.springframework.orm.jpa.JpaTransactionManager) transactions).getDataSource();
        populator.execute(source); populator.execute(source);
        assertThat(equipment.findById(asset.getId()).orElseThrow().getPurchasePrice()).isEqualByComparingTo("1000.00");
        assertThat(users.findByUsername("alice")).isPresent();
    }

    @Test void adminCanManageRolesAndRevocationAffectsExistingSessions() throws Exception {
        mvc.perform(get("/admin/users").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(get("/admin/users").with(user("alice"))).andExpect(status().isForbidden());
        mvc.perform(post("/admin/users/"+bob.getId()+"/role").with(user("alice")).with(csrf()).param("role","ADMIN")).andExpect(status().isForbidden());
        mvc.perform(post("/admin/users/"+bob.getId()+"/role").with(user("admin").roles("ADMIN")).param("role","STAFF")).andExpect(status().isForbidden());
        mvc.perform(post("/admin/users/"+bob.getId()+"/role").with(user("admin").roles("ADMIN")).with(csrf()).param("role","STAFF")).andExpect(redirectedUrl("/admin/users"));
        assertThat(users.findById(bob.getId()).orElseThrow().getRole()).isEqualTo(Role.STAFF);
        var login=mvc.perform(post("/login").with(csrf()).param("username","bob").param("password","Password123!")).andReturn();
        var session=(MockHttpSession)login.getRequest().getSession(false);
        mvc.perform(get("/admin").session(session)).andExpect(status().isOk());
        mvc.perform(get("/admin/users").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/admin/users/"+bob.getId()+"/role").with(user("admin").roles("ADMIN")).with(csrf()).param("role","USER")).andExpect(redirectedUrl("/admin/users"));
        mvc.perform(get("/admin").session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/admin/users").with(user("admin").roles("ADMIN"))).andExpect(content().string(org.hamcrest.Matchers.containsString("bob")));
    }
    @Test void lastAdminCannotBeDemoted() throws Exception {
        mvc.perform(post("/admin/users/"+admin.getId()+"/role").with(user("admin").roles("ADMIN")).with(csrf()).param("role","USER"))
            .andExpect(redirectedUrl("/admin/users")).andExpect(flash().attributeExists("error"));
        assertThat(users.findById(admin.getId()).orElseThrow().getRole()).isEqualTo(Role.ADMIN);
    }

    @Test void roleMigrationDoesNotPromoteLegacyBorrowersAndIsRepeatable() {
        jdbc.update("DELETE FROM app_migrations WHERE version='user_roles_v2'");
        jdbc.update("UPDATE users SET role='STAFF' WHERE id=?",alice.getId());
        jdbc.update("UPDATE users SET role='MANAGER' WHERE id=?",bob.getId());
        var script=new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator(new org.springframework.core.io.FileSystemResource("doc/migrations/20260926_user_roles.sql"));
        script.execute(jdbc.getDataSource()); script.execute(jdbc.getDataSource());
        assertThat(users.findById(alice.getId()).orElseThrow().getRole()).isEqualTo(Role.USER);
        assertThat(users.findById(bob.getId()).orElseThrow().getRole()).isEqualTo(Role.STAFF);
        assertThat(users.findById(admin.getId()).orElseThrow().getRole()).isEqualTo(Role.ADMIN);
    }
    @Test void bootstrapCreatesFirstAdminOnlyAndHashesPassword() {
        users.delete(admin);
        var env=new org.springframework.mock.env.MockEnvironment().withProperty("app.bootstrap-admin.username","firstadmin")
            .withProperty("app.bootstrap-admin.email","first@example.test").withProperty("app.bootstrap-admin.password","InitialPassword123!");
        var bootstrap=new com.example.itborrow.config.AdminBootstrap(jdbc,users,env,encoder);
        var tx=new TransactionTemplate(transactions);
        tx.executeWithoutResult(t -> bootstrap.run(null));
        var created=users.findByUsername("firstadmin").orElseThrow();
        assertThat(created.getRole()).isEqualTo(Role.ADMIN);
        assertThat(encoder.matches("InitialPassword123!",created.getPassword())).isTrue();
        tx.executeWithoutResult(t -> bootstrap.run(null));
        assertThat(users.countByRole(Role.ADMIN)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM role_audit WHERE actor_username='SYSTEM'",Integer.class)).isEqualTo(1);
    }
    @Test void concurrentSelfDemotionsKeepOneAdministrator() throws Exception {
        var other=account("otheradmin",Role.ADMIN);
        var pool=Executors.newFixedThreadPool(2); var gate=new CountDownLatch(1);
        try {
            var futures=List.of(admin,other).stream().map(a -> pool.submit(() -> {
                gate.await(); auth(a.getUsername());
                try { management.changeRole(a.getId(),Role.USER); return true; }
                catch(IllegalArgumentException ex) { return false; }
                finally { SecurityContextHolder.clearContext(); }
            })).toList();
            gate.countDown();
            assertThat(List.of(futures.get(0).get(15,TimeUnit.SECONDS),futures.get(1).get(15,TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);
            assertThat(users.countByRole(Role.ADMIN)).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM role_audit",Integer.class)).isEqualTo(1);
        } finally { pool.shutdownNow(); }
    }

    @Test void avatarUploadPersistsNormalizesAndIsPrivate() throws Exception {
        var source=new java.awt.image.BufferedImage(24,12,java.awt.image.BufferedImage.TYPE_INT_RGB);
        var output=new java.io.ByteArrayOutputStream(); javax.imageio.ImageIO.write(source,"png",output);
        var image=new org.springframework.mock.web.MockMultipartFile("image","photo.png","image/png",output.toByteArray());
        mvc.perform(multipart("/profile/avatar").file(image).with(user("alice"))).andExpect(status().isForbidden());
        mvc.perform(multipart("/profile/avatar").file(image).with(user("alice")).with(csrf()))
            .andExpect(redirectedUrl("/profile")).andExpect(flash().attributeExists("accountMessage"));
        mvc.perform(get("/profile/avatar").with(user("alice"))).andExpect(status().isFound())
            .andExpect(header().string("Cache-Control","no-store"));
        var bytes=org.mockito.ArgumentCaptor.forClass(byte[].class);
        org.mockito.Mockito.verify(imageStorage).upload(org.mockito.ArgumentMatchers.anyString(),bytes.capture());
        var saved=javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(bytes.getValue()));
        assertThat(saved.getWidth()).isEqualTo(256); assertThat(saved.getHeight()).isEqualTo(256);
        mvc.perform(get("/profile/avatar").with(user("bob"))).andExpect(status().isNotFound());
        mvc.perform(get("/profile").with(user("alice"))).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("class=\"site-header\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("https://storage.example.test/")));
        mvc.perform(get("/profile/change-password").with(user("alice"))).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("class=\"site-header\"")));
        mvc.perform(multipart("/profile/avatar").file(image).with(user("alice")).with(csrf())).andExpect(redirectedUrl("/profile"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_profiles WHERE user_id=? AND avatar_path IS NOT NULL",Integer.class,alice.getId())).isEqualTo(1);
    }
    @Test void invalidAvatarDoesNotReplaceExistingPicture() throws Exception {
        jdbc.update("INSERT INTO user_avatars(user_id,image_data) VALUES (?,?)",alice.getId(),new byte[]{1,2,3});
        for(var image:List.of(
            new org.springframework.mock.web.MockMultipartFile("image","fake.png","image/png","not an image".getBytes()),
            new org.springframework.mock.web.MockMultipartFile("image","huge.png","image/png",new byte[2*1024*1024+1]),
            new org.springframework.mock.web.MockMultipartFile("image","empty.png","image/png",new byte[0]))) {
            mvc.perform(multipart("/profile/avatar").file(image).with(user("alice")).with(csrf()))
                .andExpect(redirectedUrl("/profile")).andExpect(flash().attributeExists("accountError"));
        }
        assertThat(jdbc.queryForObject("SELECT image_data FROM user_avatars WHERE user_id=?",byte[].class,alice.getId())).containsExactly((byte)1,(byte)2,(byte)3);
    }

    @Test void storageFailureKeepsExistingAvatarAndShowsError() throws Exception {
        jdbc.update("INSERT INTO user_profiles(user_id,full_name,avatar_path) VALUES (?,?,?)",alice.getId(),"Alice","old.png");
        org.mockito.Mockito.doThrow(new com.example.itborrow.service.avatar.StorageException()).when(imageStorage).upload(org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.any());
        var image=new java.awt.image.BufferedImage(4,4,java.awt.image.BufferedImage.TYPE_INT_RGB);
        var bytes=new java.io.ByteArrayOutputStream(); javax.imageio.ImageIO.write(image,"png",bytes);
        mvc.perform(multipart("/profile/avatar").file(new org.springframework.mock.web.MockMultipartFile("image","a.png","image/png",bytes.toByteArray())).with(user("alice")).with(csrf()))
            .andExpect(redirectedUrl("/profile")).andExpect(flash().attributeExists("accountError"));
        assertThat(jdbc.queryForObject("SELECT avatar_path FROM user_profiles WHERE user_id=?",String.class,alice.getId())).isEqualTo("old.png");
    }
    @Test void legacyAvatarRemainsReadableAndMigrationIsRepeatable() throws Exception {
        jdbc.update("INSERT INTO user_avatars(user_id,image_data) VALUES (?,?)",alice.getId(),new byte[]{1,2,3});
        mvc.perform(get("/profile/avatar").with(user("alice"))).andExpect(status().isOk()).andExpect(content().bytes(new byte[]{1,2,3}));
        avatarService.migrateLegacy(alice.getId());
        String path=jdbc.queryForObject("SELECT avatar_path FROM user_profiles WHERE user_id=?",String.class,alice.getId());
        avatarService.migrateLegacy(alice.getId());
        assertThat(jdbc.queryForObject("SELECT avatar_path FROM user_profiles WHERE user_id=?",String.class,alice.getId())).isEqualTo(path);
        assertThat(jdbc.queryForObject("SELECT image_data FROM user_avatars WHERE user_id=?",byte[].class,alice.getId())).containsExactly((byte)1,(byte)2,(byte)3);
    }
}
