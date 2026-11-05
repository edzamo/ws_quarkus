package pe.yape.training.beneficiarios;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.yape.training.beneficiarios.domain.BeneficiaryRepository;

@QuarkusTest
class BeneficiaryResourceTest {

    @Inject
    BeneficiaryRepository repository;

    @BeforeEach
    @Transactional
    void limpiar() {
        repository.deleteAll();
    }

    private int crear(String nombre, String celular) {
        return given().contentType(ContentType.JSON)
                .body("{\"fullName\":\"%s\",\"phone\":\"%s\",\"alias\":\"x\"}".formatted(nombre, celular))
                .when().post("/beneficiaries")
                .then().statusCode(201).extract().path("id");
    }
  private int  createWithDocumentNumber(String nombre, String celular, String documentNumber) {
    return given().contentType(ContentType.JSON)
        .body("{\"fullName\":\"%s\",\"phone\":\"%s\",\"documentNumber\":\"%s\"}".formatted(nombre, celular, documentNumber))
        .when().post("/beneficiaries")
        .then().statusCode(201).extract().path("id");
  }

  private int crearIsActive(String nombre, String celular, boolean isActive) {
    return given().contentType(ContentType.JSON)
        .body("{\"fullName\":\"%s\",\"phone\":\"%s\",\"isActive\":%s}".formatted(nombre, celular, isActive))
        .when().post("/beneficiaries")
        .then().statusCode(201).extract().path("id");
  }

    @Test
    void creaYDevuelve201ConLocation() {
        given().contentType(ContentType.JSON)
                .body("{\"fullName\":\"Ana Torres\",\"phone\":\"987654321\",\"alias\":\"Ana\"}")
                .when().post("/beneficiaries")
                .then().statusCode(201)
                .header("Location", containsString("/beneficiaries/"))
                .body("id", notNullValue())
                .body("fullName", is("Ana Torres"));
    }

    @Test
    void obtienePorId() {
        int id = crear("Luis Quispe", "912345678");
        given().when().get("/beneficiaries/" + id)
                .then().statusCode(200).body("phone", is("912345678"));
    }

    @Test
    void obtienePorCelular() {
        int id = crear("Luis Quispe", "912345678");
        given().when().get("/beneficiaries/by-phone/912345678")
                .then().statusCode(200).body("id", is(id));
    }

    @Test
    void idInexistenteDevuelve404ConCuerpoDeError() {
        given().when().get("/beneficiaries/9999")
                .then().statusCode(404).body("code", is("NOT_FOUND"));
    }

    @Test
    void celularInvalidoDevuelve400() {
        given().contentType(ContentType.JSON)
                .body("{\"fullName\":\"Ana\",\"phone\":\"12345\"}")
                .when().post("/beneficiaries")
                .then().statusCode(400)
                .body("code", is("VALIDATION_ERROR"))
                .body("message", containsString("phone"));
    }

    @Test
    void nombreVacioDevuelve400() {
        given().contentType(ContentType.JSON)
                .body("{\"fullName\":\"\",\"phone\":\"987654321\"}")
                .when().post("/beneficiaries")
                .then().statusCode(400).body("message", containsString("fullName"));
    }

    @Test
    void celularDuplicadoDevuelve409() {
        crear("Ana", "987654321");
        given().contentType(ContentType.JSON)
                .body("{\"fullName\":\"Otra Ana\",\"phone\":\"987654321\"}")
                .when().post("/beneficiaries")
                .then().statusCode(409).body("code", is("DUPLICATE_PHONE"));
    }

    @Test
    void documentNumberDuplicadoDevuelve409() {
      createWithDocumentNumber("Ana", "987654321", "12345678");
      given().contentType(ContentType.JSON)
                .body("{\"fullName\":\"Otra Ana\",\"phone\":\"912345678\",\"documentNumber\":\"12345678\"}")
                .when().post("/beneficiaries")
                .then().statusCode(409).body("code", is("DUPLICATE_DOCUMENT_NUMBER"));
    }



  @Test
    void actualiza() {
        int id = crear("Ana", "987654321");
        given().contentType(ContentType.JSON)
                .body("{\"fullName\":\"Ana María\",\"phone\":\"987654321\",\"alias\":\"AM\"}")
                .when().put("/beneficiaries/" + id)
                .then().statusCode(200).body("fullName", is("Ana María")).body("alias", is("AM"));
    }

    @Test
    void actualizarConCelularDeOtroDevuelve409() {
        crear("Ana", "987654321");
        int otro = crear("Luis", "912345678");
        given().contentType(ContentType.JSON)
                .body("{\"fullName\":\"Luis\",\"phone\":\"987654321\"}")
                .when().put("/beneficiaries/" + otro)
                .then().statusCode(409);
    }

    @Test
    void eliminaYLuegoDa404() {
        int id = crear("Ana", "987654321");
        given().when().delete("/beneficiaries/" + id).then().statusCode(204);
        given().when().get("/beneficiaries/" + id).then().statusCode(404);
        given().when().delete("/beneficiaries/" + id).then().statusCode(404);
    }


    @Test
    void listaConBusquedaYPaginacion() {
        crear("Ana Torres", "987654321");
        crear("Luis Quispe", "912345678");
        crear("Ana Ramos", "955555555");

        given().when().get("/beneficiaries")
                .then().statusCode(200).body("size()", is(3));
        given().queryParam("q", "ana").when().get("/beneficiaries")
                .then().body("size()", is(2)).body("fullName", contains("Ana Ramos", "Ana Torres"));
        given().queryParam("size", 2).queryParam("page", 1).when().get("/beneficiaries")
                .then().body("size()", is(1));
    }

    @Test
    void openApiExpuesto() {
        given().when().get("/q/openapi").then().statusCode(200).body(containsString("/beneficiaries"));
    }
}
