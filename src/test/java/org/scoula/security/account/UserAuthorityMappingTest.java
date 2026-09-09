package org.scoula.security.account;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import org.scoula.member.mapper.MemberMapper;
import org.scoula.security.account.domain.AuthVO;
import org.scoula.security.account.domain.CustomUser;
import org.scoula.security.account.domain.MemberVO;
import org.scoula.security.account.mapper.UserDetailsMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserAuthorityMappingTest {

    @Test
    void customUserUsesDatabaseMappedRoleAsGrantedAuthority() {
        AuthVO authority = new AuthVO();
        authority.setAuth("ROLE_OWNER");
        CustomUser user = new CustomUser(MemberVO.builder()
                .username("owner@example.com")
                .password("unused")
                .role("OWNER")
                .placeId(10L)
                .authList(List.of(authority))
                .build());

        assertEquals(List.of("ROLE_OWNER"), user.getAuthorities().stream()
                .map(a -> a.getAuthority()).toList());
        assertEquals(10L, user.getMember().getPlaceId());
    }

    @Test
    void authenticationQueriesBuildAuthoritiesFromRoleColumn() throws Exception {
        Configuration configuration = configuration();
        parse(configuration, "org/scoula/security/account/mapper/UserDetailsMapper.xml");

        String byEmail = sql(configuration, UserDetailsMapper.class.getName() + ".get");
        String byId = sql(configuration, UserDetailsMapper.class.getName() + ".getById");
        for (String sql : List.of(byEmail, byId)) {
            assertTrue(sql.contains("CONCAT('ROLE_', role) AS auth"));
            assertTrue(sql.contains("role"));
            assertTrue(sql.contains("place_id"));
            assertFalse(sql.contains("ROLE_MEMBER"));
        }
    }

    @Test
    void registrationAlwaysInsertsUserRoleAndNoPlace() throws Exception {
        Configuration configuration = configuration();
        parse(configuration, "org/scoula/member/mapper/MemberMapper.xml");

        for (String id : List.of("insert", "insertSocial")) {
            String sql = sql(configuration, MemberMapper.class.getName() + "." + id);
            assertTrue(sql.contains("role"));
            assertTrue(sql.contains("place_id"));
            assertTrue(sql.contains("'USER'"));
            assertTrue(sql.contains("NULL"));
        }
    }

    @Test
    void socialAccountLookupLoadsRoleAndPlace() throws Exception {
        Configuration configuration = configuration();
        parse(configuration, "org/scoula/member/mapper/MemberMapper.xml");

        String sql = sql(configuration, MemberMapper.class.getName() + ".findByProvider");
        assertTrue(sql.contains("role"));
        assertTrue(sql.contains("place_id"));
        assertTrue(sql.contains("CONCAT('ROLE_', role) AS auth"));
    }

    private Configuration configuration() {
        Configuration configuration = new Configuration();
        configuration.getTypeAliasRegistry().registerAlias("AuthVO", AuthVO.class);
        configuration.getTypeAliasRegistry().registerAlias("MemberVO", MemberVO.class);
        return configuration;
    }

    private void parse(Configuration configuration, String path) throws Exception {
        try (var input = getClass().getClassLoader().getResourceAsStream(path)) {
            new XMLMapperBuilder(input, configuration, path, configuration.getSqlFragments()).parse();
        }
    }

    private String sql(Configuration configuration, String statement) {
        return configuration.getMappedStatement(statement).getBoundSql(new Object())
                .getSql().replaceAll("\\s+", " ");
    }
}
