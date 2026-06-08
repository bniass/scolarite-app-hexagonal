# WSO2 API Manager + Keycloak — Guide d'intégration complet
### Application : `scolarite-app-hexagonal`

---

## Table des matières

1. [Architecture cible](#1-architecture-cible)
2. [Prérequis et ports](#2-prérequis-et-ports)
3. [Docker Compose — Configuration](#3-docker-compose--configuration)
4. [Keycloak — Configuration du realm](#4-keycloak--configuration-du-realm)
5. [WSO2 — Ajout du Key Manager Keycloak](#5-wso2--ajout-du-key-manager-keycloak)
6. [WSO2 — Modification deployment.toml](#6-wso2--modification-deploymenttoml)
7. [WSO2 — Création et publication des APIs](#7-wso2--création-et-publication-des-apis)
8. [WSO2 — Application DevPortal et génération des clés](#8-wso2--application-devportal-et-génération-des-clés)
9. [Spring Boot — SecurityConfig par service](#9-spring-boot--securityconfig-par-service)
10. [Spring Boot — application.yml](#10-spring-boot--applicationyml)
11. [Spring Boot — OpenApiConfig (Swagger)](#11-spring-boot--openapiconfig-swagger)
12. [Spring Boot — common-service beans](#12-spring-boot--common-service-beans)
13. [Machine hôte — /etc/hosts](#13-machine-hôte--etchosts)
14. [Flux de bout en bout — comment obtenir un token et appeler une API](#14-flux-de-bout-en-bout--comment-obtenir-un-token-et-appeler-une-api)
15. [Tableau des erreurs et solutions](#15-tableau-des-erreurs-et-solutions)
16. [Vérifications rapides (commandes de diagnostic)](#16-vérifications-rapides-commandes-de-diagnostic)

---

## 1. Architecture cible

```
[Client / Swagger]
       │
       │  POST http://keycloak:8180/realms/scolarite/.../token
       │  → access_token (JWT, iss = http://keycloak:8180/realms/scolarite)
       │
       ▼
[WSO2 API Gateway :8243]
   1. Reçoit : Authorization: Bearer <keycloak-jwt>
   2. Identifie le Key Manager via claim "iss"
   3. Valide la signature JWT via JWKS Keycloak (self-validation)
   4. Transmet la requête + Authorization header au backend
       │
       ▼
[Spring Boot service :808x]
   1. Reçoit : Authorization: Bearer <keycloak-jwt>
   2. Valide la signature via jwk-set-uri Keycloak
   3. Vérifie l'issuer
   4. Extrait les rôles depuis realm_access.roles
   5. Applique les règles RBAC (admin/super/user)
```

**Point critique** : Le claim `iss` du JWT **doit** correspondre à l'URL interne Docker `http://keycloak:8180/realms/scolarite`, pas `http://localhost:8180/...`. C'est la source principale des erreurs 900901.

---

## 2. Prérequis et ports

| Service | URL externe | URL interne Docker | Port |
|---|---|---|---|
| Keycloak | `http://localhost:8180` | `http://keycloak:8180` | 8180 |
| WSO2 Publisher/DevPortal | `https://localhost:9443` | — | 9443 |
| WSO2 Gateway HTTPS | `https://localhost:8243` | — | 8243 |
| annee-academique-service | `http://localhost:8080` | `http://host.docker.internal:8080` | 8080 |
| etudiant-service | `http://localhost:8092` | `http://host.docker.internal:8092` | 8092 |
| school-service | `http://localhost:8091` | `http://host.docker.internal:8091` | 8091 |
| paiement-service | `http://localhost:8093` | `http://host.docker.internal:8093` | 8093 |
| inscrption-service | `http://localhost:8094` | `http://host.docker.internal:8094` | 8094 |

**Identifiants** : `admin` / `admin` pour WSO2 et Keycloak.

---

## 3. Docker Compose — Configuration

```yaml
# docker-compose.yml (extraits essentiels)

services:
  keycloak-scolarite:
    image: quay.io/keycloak/keycloak:24.0
    container_name: keycloak-scolarite
    hostname: keycloak          # ← CRITIQUE : le hostname doit être "keycloak"
    command: start-dev
    environment:
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin
      KC_HTTP_PORT: 8180
      KC_HOSTNAME_STRICT: "false"
      KC_HOSTNAME_STRICT_HTTPS: "false"
    ports:
      - "8180:8180"
    volumes:
      - wso2am-keycloak:/opt/keycloak/data
    networks:
      - scolarite-net

  wso2am:
    image: wso2am:4.7.0
    container_name: wso2am-scolarite
    hostname: wso2am
    ports:
      - "9443:9443"   # Publisher / DevPortal / Admin (HTTPS)
      - "8243:8243"   # Gateway HTTPS
      - "8280:8280"   # Gateway HTTP
    extra_hosts:
      - "host.docker.internal:host-gateway"  # ← accès aux services Spring Boot sur l'hôte
    volumes:
      - "./volumes/wso2am/repository/logs:/home/wso2carbon/wso2am-4.7.0/repository/logs"
      - wso2am-database:/home/wso2carbon/wso2am-4.7.0/repository/database
      - wso2am-solr:/home/wso2carbon/solr
    networks:
      - scolarite-net

networks:
  scolarite-net:
    driver: bridge    # ← Keycloak et WSO2 sur le même réseau Docker

volumes:
  wso2am-database:
  wso2am-solr:
  wso2am-keycloak:
```

> **Pourquoi `hostname: keycloak` est critique ?**  
> Keycloak intègre son hostname dans le claim `iss` du JWT. Si le hostname est `keycloak`, alors `iss = http://keycloak:8180/realms/scolarite`.  
> WSO2 et les services Spring Boot utilisent aussi `http://keycloak:8180/...` pour valider. Tout doit correspondre.

---

## 4. Keycloak — Configuration du realm

### 4.1 Créer le realm `scolarite`

1. Aller sur `http://localhost:8180/admin/master/console/`
2. Login : `admin` / `admin`
3. Menu haut gauche → **Create Realm**
4. Nom : `scolarite` → **Create**

### 4.2 Créer les rôles realm

Dans le realm `scolarite` → **Realm roles** → **Create role** :

| Rôle | Description |
|---|---|
| `super` | Administrateur complet (CRUD) |
| `admin` | Administrateur limité (lecture + création) |
| `user` | Utilisateur lecture seule |

### 4.3 Créer les utilisateurs

Dans **Users** → **Create new user** :

| Username | Rôles assignés | Usage |
|---|---|---|
| `baye` | `super`, `admin`, `user` | Tests complets |
| `mouha` | `admin`, `user` | Tests admin |
| `barham` | `user` | Tests lecture |

Pour assigner les rôles : ouvrir l'utilisateur → onglet **Role mappings** → **Assign role** → sélectionner les rôles realm.

Pour définir un mot de passe : onglet **Credentials** → **Set password** → désactiver "Temporary".

### 4.4 Créer le client DCR pour WSO2

Ce client permettra à WSO2 d'enregistrer dynamiquement des applications OAuth2 (DCR).

1. **Clients** → **Create client**
2. **Client ID** : `wso2-dcr`
3. **Client authentication** : ON (confidential)
4. **Authentication flow** : cocher `Service accounts roles`
5. **Save**

Dans l'onglet **Service account roles** du client `wso2-dcr` → **Assign role** → chercher `manage-clients`, `manage-realm`, `view-clients` (rôles de `realm-management`).

Récupérer le **secret** dans l'onglet **Credentials** du client `wso2-dcr`.

### 4.5 Créer le client scope `openid` (si absent)

Si WSO2 envoie `scope=openid` lors du DCR et que Keycloak retourne une erreur :

1. **Client scopes** → **Create client scope**
2. Nom : `openid`, Type : `Default`
3. Ajouter ce scope au client `wso2-dcr` : onglet **Client scopes** → **Add client scope**

### 4.6 Client `swagger-ui` pour les tests Swagger (optionnel)

1. **Clients** → **Create client**
2. **Client ID** : `swagger-ui`
3. **Client authentication** : OFF (public)
4. **Authentication flow** : `Standard flow`, `Direct access grants`
5. **Valid redirect URIs** : `http://localhost:*`, `https://localhost:*`
6. **Web origins** : `http://localhost:*`, `+`
7. **Save**

---

## 5. WSO2 — Ajout du Key Manager Keycloak

### 5.1 Accéder au portail Admin WSO2

`https://localhost:9443/admin` → login `admin` / `admin`

### 5.2 Créer le Key Manager

**Key Managers** → **Add Key Manager**

| Champ | Valeur |
|---|---|
| **Name** | `Keycloak` |
| **Display Name** | `Keycloak` |
| **Key Manager Type** | `KeyCloak` |
| **Well-known URL** | `http://keycloak:8180/realms/scolarite/.well-known/openid-configuration` |

Cliquer **Import** pour auto-remplir les endpoints depuis la well-known URL.

Vérifier que les champs suivants sont correctement remplis :

| Champ | Valeur attendue |
|---|---|
| **Issuer** | `http://keycloak:8180/realms/scolarite` |
| **Client Registration Endpoint** | `http://keycloak:8180/realms/scolarite/clients-registrations/openid-connect` |
| **Introspection Endpoint** | `http://keycloak:8180/realms/scolarite/protocol/openid-connect/token/introspect` |
| **Token Endpoint** | `http://keycloak:8180/realms/scolarite/protocol/openid-connect/token` |
| **Revoke Endpoint** | `http://keycloak:8180/realms/scolarite/protocol/openid-connect/revoke` |
| **JWKS URI** | `http://keycloak:8180/realms/scolarite/protocol/openid-connect/certs` |

Dans la section **Connector Configurations** :
- **client_id** : `wso2-dcr`
- **client_secret** : `<secret du client wso2-dcr récupéré dans Keycloak>`

Dans la section **Token Validation** :
- Cocher **Self Validate JWT** : ✅
- **Certificate Type** : `JWKS`
- **Certificate URL** : `http://keycloak:8180/realms/scolarite/protocol/openid-connect/certs`

Cliquer **Add**.

### 5.3 Vérification via API

```bash
curl -sk "https://localhost:9443/api/am/admin/v4/key-managers/94b8f3c4-e923-4d0b-bdb6-ced35bd6ac45" \
  -u "admin:admin" | python3 -m json.tool
```

Le retour doit avoir :
```json
{
  "name": "Keycloak",
  "type": "KeyCloak",
  "enabled": true,
  "issuer": "http://keycloak:8180/realms/scolarite",
  "enableSelfValidationJWT": true,
  "tokenType": "DIRECT"
}
```

---

## 6. WSO2 — Modification deployment.toml

> **⚠️ Point critique n°2** : Par défaut, WSO2 **supprime** l'`Authorization` header avant d'envoyer la requête au backend. Il faut activer le transfert.

### 6.1 Modifier le fichier dans le container

```bash
# Entrer dans le container WSO2
docker exec -it wso2am-scolarite bash

# Éditer deployment.toml
vi /home/wso2carbon/wso2am-4.7.0/repository/conf/deployment.toml
```

### 6.2 Ajouter en fin de fichier

```toml
[apim.oauth_config]
enable_outbound_auth_header = true
```

> **Pourquoi ?** Sans cette ligne, WSO2 valide le JWT à la gateway mais n'envoie PAS le header `Authorization: Bearer <token>` au service Spring Boot backend. Spring Boot reçoit donc une requête sans token → 401.

### 6.3 Redémarrer WSO2

```bash
docker restart wso2am-scolarite
```

Attendre ~2 minutes que WSO2 redémarre complètement. Vérifier :

```bash
curl -sk -o /dev/null -w "%{http_code}" "https://localhost:9443/api/am/publisher/v4/apis" -u "admin:admin"
# → 200
```

---

## 7. WSO2 — Création et publication des APIs

### 7.1 Publisher

`https://localhost:9443/publisher` → **Create API** → **Start from Scratch**

Pour chaque microservice :

| API Name | Context | Version | Backend URL |
|---|---|---|---|
| `annee-academique-service-api` | `/annee-academique-service-api` | `v1` | `http://host.docker.internal:8080` |
| `etudiant-service-api` | `/etudiant-service-api` | `v1` | `http://host.docker.internal:8092` |
| `school-service-api` | `/school-service-api` | `v1` | `http://host.docker.internal:8091` |
| `paiement-service-api` | `/paiement-service-api` | `v1` | `http://host.docker.internal:8093` |
| `Inscription-Service-api` | `/Inscription-Service-api` | `v1` | `http://host.docker.internal:8094` |

> **Important** : L'URL backend utilise `host.docker.internal` pour accéder aux services Spring Boot qui tournent sur la machine hôte, depuis le container WSO2.

### 7.2 Configurer les ressources (routes)

Dans l'onglet **Resources** de chaque API, définir les routes correspondant aux controllers Spring Boot.

Exemple pour `annee-academique-service-api` :
```
POST  /api/academic-years
POST  /api/academic-years/{code}/update
GET   /api/academic-years/{code}/publish
GET   /api/academic-years/{code}/open-enrollments
GET   /api/academic-years/{code}/close
GET   /api/academic-years/{code}/close-enrollments
```

### 7.3 Configurer la sécurité de l'API

Dans **Runtime** → **Application Level Security** :
- Cocher **OAuth2** ✅
- Le reste par défaut

### 7.4 Publier l'API

**Lifecycle** → **Publish**

---

## 8. WSO2 — Application DevPortal et génération des clés

### 8.1 Créer l'application

`https://localhost:9443/devportal` → **Applications** → **Add Application**

| Champ | Valeur |
|---|---|
| **Name** | `annee-academique-api` (ou un nom global) |
| **Throttling Policy** | `Unlimited` |

### 8.2 S'abonner aux APIs

Dans l'application → **Subscriptions** → **Subscribe APIs** → sélectionner toutes les APIs publiées.

### 8.3 Générer les clés de production (Key Manager Keycloak)

Dans l'application → **Production Keys** → onglet **KEYCLOAK** (pas "Resident Key Manager") :

- Si le bouton **GENERATE KEYS** est présent → cliquer dessus
- WSO2 appelle le DCR Keycloak et crée un client dans le realm `scolarite`
- Une fois généré, noter le **Consumer Key** (ex: `ff59b33d-fe37-46de-b5de-8f68bee27632`)

### 8.4 Récupérer le Consumer Secret

Le Consumer Key = Client ID dans Keycloak. Pour récupérer le secret :

```bash
# 1. Obtenir un token admin Keycloak
ADMIN_TOKEN=$(curl -s -X POST "http://localhost:8180/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&client_id=admin-cli&username=admin&password=admin" \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")

# 2. Récupérer le secret du client (remplacer par votre Consumer Key)
curl -s "http://localhost:8180/admin/realms/scolarite/clients/ff59b33d-fe37-46de-b5de-8f68bee27632/client-secret" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  | python3 -c "import sys,json; print('Secret:', json.load(sys.stdin)['value'])"
```

### 8.5 Corriger l'auth method du client Keycloak DCR

Le client créé par WSO2 via DCR peut utiliser `client_secret_jwt` par défaut. Il faut le passer en `client-secret` (basic/post) :

```bash
ADMIN_TOKEN=$(curl -s -X POST "http://localhost:8180/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&client_id=admin-cli&username=admin&password=admin" \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")

# Remplacer l'UUID par votre Consumer Key
curl -s -X PUT "http://localhost:8180/admin/realms/scolarite/clients/ff59b33d-fe37-46de-b5de-8f68bee27632" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"clientAuthenticatorType":"client-secret"}'
```

---

## 9. Spring Boot — SecurityConfig par service

### Modèle commun (à répliquer dans chaque service)

```java
package com.ecole221.<service>.infrastructure.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.*;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Swagger toujours public
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                // ↓ Adapter les règles selon le service (voir tableau ci-dessous)
                .requestMatchers("POST", "/api/**").hasRole("super")
                .requestMatchers("PUT", "/api/**").hasRole("super")
                .requestMatchers("GET", "/api/**").hasAnyRole("admin", "super")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            );
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::extractRoles);
        return converter;
    }

    // ← CRITIQUE : extraire les rôles depuis realm_access.roles (Keycloak)
    // Spring Boot par défaut lit le claim "scope", pas "realm_access.roles"
    private Collection<GrantedAuthority> extractRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null || !realmAccess.containsKey("roles")) {
            return Collections.emptyList();
        }
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) realmAccess.get("roles");
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
    }
}
```

### Règles RBAC par service

| Service | DELETE | POST | PUT | GET |
|---|---|---|---|---|
| `annee-academique-service` | — | `super` | `super` | `admin`, `super` |
| `etudiant-service` | `super` | `admin`, `super` | `admin`, `super` | `admin`, `super`, `user` |
| `school-service` | `super` | `super` | `super` | `admin`, `super`, `user` |
| `paiement-service` | — | `admin`, `super` | — | `admin`, `super`, `user` |
| `inscrption-service` | `super` | `admin`, `super` | `admin`, `super` | `admin`, `super`, `user` |

---

## 10. Spring Boot — application.yml

Ajouter dans chaque service (adapter le profil actif) :

```yaml
spring:
  main:
    # Nécessaire si common-service définit aussi jwtAuthenticationConverter
    allow-bean-definition-overriding: true

  application:
    name: <nom-du-service>

  profiles:
    active: mysql,dev   # "dev" active Wso2JwtDecoderConfig dans common-service

  security:
    oauth2:
      resourceserver:
        jwt:
          # URL interne Docker — DOIT correspondre à l'issuer du JWT Keycloak
          jwk-set-uri: http://keycloak:8180/realms/scolarite/protocol/openid-connect/certs
          issuer-uri: http://keycloak:8180/realms/scolarite
```

> **Pourquoi `http://keycloak:8180` et pas `http://localhost:8180` ?**  
> Le JWT généré par Keycloak contient `"iss": "http://keycloak:8180/realms/scolarite"` (car le hostname Docker est `keycloak`).  
> Spring Boot vérifie que la valeur de `issuer-uri` **correspond exactement** au claim `iss` du JWT.  
> Si on met `localhost:8180` → validation échoue → 401.  
> La machine hôte doit résoudre `keycloak` → voir section 13.

---

## 11. Spring Boot — OpenApiConfig (Swagger)

```java
package com.ecole221.<service>.infrastructure.web.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.OAuthFlow;
import io.swagger.v3.oas.annotations.security.OAuthFlows;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(title = "<Nom Service> API", version = "v1"),
        security = @SecurityRequirement(name = "keycloak")
)
@SecurityScheme(
        name = "keycloak",
        type = SecuritySchemeType.OAUTH2,
        flows = @OAuthFlows(
                password = @OAuthFlow(
                        // Utiliser keycloak:8180 (pas localhost)
                        authorizationUrl = "http://keycloak:8180/realms/scolarite/protocol/openid-connect/auth",
                        tokenUrl = "http://keycloak:8180/realms/scolarite/protocol/openid-connect/token"
                )
        )
)
public class OpenApiConfig {
}
```

> **Note Swagger** : Pour que Swagger UI puisse appeler Keycloak depuis le navigateur,  
> `keycloak` doit être résolu par le navigateur. Ajouter `127.0.0.1 keycloak` dans `/etc/hosts`.

---

## 12. Spring Boot — common-service beans

Le `common-service` définit ces beans partagés (profil `dev`) :

### `Wso2JwtDecoderConfig.java`
```java
@Configuration
@Profile("dev")
public class Wso2JwtDecoderConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRolesConverter());
        return converter;
    }
}
```

### `KeycloakRolesConverter.java`
```java
public class KeycloakRolesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null || !realmAccess.containsKey("roles")) {
            return Collections.emptyList();
        }
        List<String> roles = (List<String>) realmAccess.get("roles");
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
    }
}
```

> **Conflit de beans** : `Wso2JwtDecoderConfig` et `SecurityConfig` définissent tous les deux `jwtAuthenticationConverter`. Résolution : `spring.main.allow-bean-definition-overriding: true` dans application.yml. Le bean chargé en dernier gagne — les deux convertisseurs extraient les rôles depuis `realm_access.roles` donc le comportement est identique.

---

## 13. Machine hôte — /etc/hosts

Ajouter cette ligne pour que `keycloak` soit résolu depuis la machine hôte (Swagger, curl, Spring Boot) :

```
127.0.0.1 keycloak
```

```bash
# Sur macOS / Linux
sudo sh -c 'echo "127.0.0.1 keycloak" >> /etc/hosts'

# Vérification
grep keycloak /etc/hosts
# → 127.0.0.1 keycloak
```

> **Pourquoi ?** Spring Boot tourne sur la machine hôte et doit valider les JWT en accédant à `http://keycloak:8180/...`. Sans cette entrée, `keycloak` n'est pas résolvable depuis l'hôte.

---

## 14. Flux de bout en bout — comment obtenir un token et appeler une API

### 14.1 Informations d'identification (à noter lors de la configuration)

```
Consumer Key   : ff59b33d-fe37-46de-b5de-8f68bee27632   ← Client ID dans Keycloak
Consumer Secret: rDjyWMMsVF5FjqYii1r4baSdVF7xSLHj      ← Secret Keycloak
```

> Ces valeurs sont spécifiques à votre instance. Récupérer le Consumer Key dans WSO2 DevPortal → Application → Production Keys → onglet KEYCLOAK. Récupérer le secret via l'API admin Keycloak (voir section 8.4).

### 14.2 Commandes curl complètes

```bash
# ─── Étape 1 : Obtenir un token Keycloak (password grant) ───────────────────
TOKEN=$(curl -s -X POST \
  "http://keycloak:8180/realms/scolarite/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=ff59b33d-fe37-46de-b5de-8f68bee27632" \
  -d "client_secret=rDjyWMMsVF5FjqYii1r4baSdVF7xSLHj" \
  -d "username=baye" \
  -d "password=admin" \
  -d "scope=openid" \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")

echo "Token: ${TOKEN:0:50}..."

# ─── Vérifier le contenu du token ────────────────────────────────────────────
echo $TOKEN | cut -d. -f2 | base64 -d 2>/dev/null \
  | python3 -c "import sys,json; d=json.load(sys.stdin); print('iss:', d['iss']); print('roles:', d['realm_access']['roles'])"

# ─── Étape 2 : Appeler l'API via WSO2 Gateway ────────────────────────────────

# GET (nécessite rôle admin ou super)
curl -sk -X GET \
  "https://localhost:8243/etudiant-service-api/v1/api/etudiants" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Accept: application/json"

# POST (nécessite rôle super pour annee-academique)
curl -sk -X POST \
  "https://localhost:8243/annee-academique-service-api/v1/api/academic-years" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "2025",
    "dateDebut": "2025-10-01",
    "dateFin": "2026-06-30"
  }'
```

### 14.3 Test direct sur Spring Boot (sans WSO2)

```bash
# Appel direct au service (bypass WSO2)
curl -s -X GET "http://localhost:8092/api/etudiants" \
  -H "Authorization: Bearer $TOKEN"
```

---

## 15. Tableau des erreurs et solutions

| Code erreur | Message | Cause | Solution |
|---|---|---|---|
| `900900` | Unclassified Authentication Failure | L'`iss` du JWT ne correspond pas à l'issuer du Key Manager WSO2 | Obtenir le token depuis `keycloak:8180` (pas `localhost:8180`) |
| `900901` | Invalid Credentials | (a) Consumer Key inconnu dans Keycloak, (b) Issuer mismatch | (a) Utiliser le bon Consumer Key depuis l'onglet KEYCLOAK, (b) voir 900900 |
| `900908` | Resource Forbidden | L'application n'est pas abonnée à l'API | DevPortal → Subscriptions → s'abonner |
| `303001` | Backend Suspended | Le backend Spring Boot est inaccessible ou sur le mauvais port | Corriger l'URL backend dans WSO2 Publisher (ex: `host.docker.internal:8080`) |
| `405` | Method Not Allowed | La méthode HTTP n'est pas définie dans les ressources WSO2 | Ajouter la route dans le Publisher WSO2 |
| `401` Spring Boot | — | (a) WSO2 ne transmet pas l'Authorization header, (b) issuer mismatch | (a) `enable_outbound_auth_header = true` dans deployment.toml, (b) corriger `issuer-uri` |
| `403` insufficient_scope | — | Spring Boot lit `scope` au lieu de `realm_access.roles` | Implémenter `jwtAuthenticationConverter` avec extraction de `realm_access.roles` |
| Bean conflict | jwtAuthenticationConverter already registered | Double définition du bean (SecurityConfig + Wso2JwtDecoderConfig) | `spring.main.allow-bean-definition-overriding: true` |
| `invalid_client` | Parameter client_assertion_type is missing | Keycloak client créé par DCR utilise `client_secret_jwt` | Changer `clientAuthenticatorType` en `client-secret` via API admin Keycloak |
| TypeError: Failed to fetch | (Swagger) | CORS entre navigateur et Keycloak | Ajouter `http://localhost:*` aux Web Origins du client `swagger-ui` dans Keycloak |

---

## 16. Vérifications rapides (commandes de diagnostic)

### Vérifier la connectivité WSO2 → Keycloak

```bash
docker exec wso2am-scolarite curl -s \
  "http://keycloak:8180/realms/scolarite/.well-known/openid-configuration" \
  | python3 -c "import sys,json; d=json.load(sys.stdin); print('issuer:', d['issuer'])"
# → issuer: http://keycloak:8180/realms/scolarite
```

### Vérifier le contenu d'un JWT

```bash
# Décoder le payload (partie centrale du token JWT)
echo "<TOKEN>" | cut -d. -f2 | base64 -d 2>/dev/null \
  | python3 -m json.tool | grep -E "iss|azp|realm_access|exp"
```

### Lister les clients Keycloak dans le realm scolarite

```bash
ADMIN_TOKEN=$(curl -s -X POST "http://localhost:8180/realms/master/protocol/openid-connect/token" \
  -d "grant_type=password&client_id=admin-cli&username=admin&password=admin" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")

curl -s "http://localhost:8180/admin/realms/scolarite/clients?max=20" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  | python3 -c "import sys,json; [print(c['clientId'], '-', c.get('name','')) for c in json.load(sys.stdin)]"
```

### Vérifier la config deployment.toml WSO2

```bash
docker exec wso2am-scolarite tail -5 \
  /home/wso2carbon/wso2am-4.7.0/repository/conf/deployment.toml
# Doit contenir :
# [apim.oauth_config]
# enable_outbound_auth_header = true
```

### Tester le flow complet en une commande

```bash
# Remplacer CONSUMER_KEY, CONSUMER_SECRET, USERNAME, PASSWORD, API_PATH
CONSUMER_KEY="ff59b33d-fe37-46de-b5de-8f68bee27632"
CONSUMER_SECRET="rDjyWMMsVF5FjqYii1r4baSdVF7xSLHj"
USERNAME="baye"
PASSWORD="admin"

TOKEN=$(curl -s -X POST "http://keycloak:8180/realms/scolarite/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&client_id=${CONSUMER_KEY}&client_secret=${CONSUMER_SECRET}&username=${USERNAME}&password=${PASSWORD}&scope=openid" \
  | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('access_token','ERREUR: '+str(d)))")

curl -sk "https://localhost:8243/etudiant-service-api/v1/api/etudiants" \
  -H "Authorization: Bearer $TOKEN" \
  -w "\nHTTP %{http_code}\n"
```

---

## Résumé des points critiques

```
1. hostname: keycloak     → dans docker-compose (iss du JWT doit être keycloak:8180)
2. /etc/hosts             → 127.0.0.1 keycloak (pour l'hôte)
3. KeyManager Keycloak    → type KeyCloak, self-validate JWT, JWKS Keycloak
4. deployment.toml WSO2   → [apim.oauth_config] enable_outbound_auth_header = true
5. application.yml        → issuer-uri = http://keycloak:8180/realms/scolarite
6. SecurityConfig         → jwtAuthenticationConverter() lisant realm_access.roles
7. allow-bean-definition-overriding: true  → éviter le conflit de beans
8. Consumer Key           → utiliser l'onglet KEYCLOAK (pas Resident Key Manager)
9. client auth method     → clientAuthenticatorType: client-secret (pas client_secret_jwt)
```
