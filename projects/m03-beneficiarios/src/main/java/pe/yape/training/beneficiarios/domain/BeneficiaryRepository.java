package pe.yape.training.beneficiarios.domain;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pe.yape.training.beneficiarios.error.BeneficiaryNotFoundException;
import pe.yape.training.beneficiarios.error.BeneficiaryPhoneNotFoundException;

/**
 * Patrón Repository con Panache (≈ Spring Data JpaRepository).
 * PanacheRepository ya trae: persist, findById, listAll, deleteById, count, find(...), etc.
 */
@ApplicationScoped
public class BeneficiaryRepository implements PanacheRepository<Beneficiary> {

  private static final Logger log = LoggerFactory.getLogger(BeneficiaryRepository.class);


  public Optional<Beneficiary> findByIdIsActive(Long id) {
    return find("id = ?1 and isActive = true", id).firstResultOptional();

    }
  public Optional<Beneficiary> findByPhone(String phone) {
        return find("phone", phone).firstResultOptional();
    }

    public  Optional <Beneficiary> findByDocumentNumber(String documentNumber) {
        return find("documentNumber", documentNumber).firstResultOptional();
    }

    public List<Beneficiary> search(String text, int page, int size) {
        var query = (text == null || text.isBlank())
                ? findAll(Sort.by("fullName"))
                : find("lower(fullName) like ?1", Sort.by("fullName"), "%" + text.toLowerCase() + "%");
        return query.page(Page.of(page, size)).list();
    }

    public Beneficiary getByPhone(String phone) {
      return findByPhone(phone).orElseThrow(() -> new BeneficiaryPhoneNotFoundException(phone));
    }



}
