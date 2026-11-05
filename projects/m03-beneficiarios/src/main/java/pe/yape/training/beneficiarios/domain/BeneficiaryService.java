package pe.yape.training.beneficiarios.domain;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;
import pe.yape.training.beneficiarios.api.BeneficiaryRequest;
import pe.yape.training.beneficiarios.error.BeneficiaryNotFoundException;
import pe.yape.training.beneficiarios.error.DuplicateDocumentNumberException;
import pe.yape.training.beneficiarios.error.DuplicatePhoneException;

/**
 * Lógica de negocio y fronteras transaccionales.
 * @Transactional es jakarta.transaction (≈ @Transactional de Spring).
 */
@ApplicationScoped
public class BeneficiaryService {

    private final BeneficiaryRepository repository;

    BeneficiaryService(BeneficiaryRepository repository) {
        this.repository = repository;
    }

    public List<Beneficiary> list(String text, int page, int size) {
        return repository.search(text, page, size);
    }

    public Beneficiary get(Long id) {
        Beneficiary b = repository.findById(id);
        if (b == null) {
            throw new BeneficiaryNotFoundException(id);
        }
        return b;
    }

    public Beneficiary getByPhone(String phone) {
        return repository.getByPhone(phone);
    }

    @Transactional
    public Beneficiary create(BeneficiaryRequest req) {
        repository.findByPhone(req.phone()).ifPresent(x -> {
            throw new DuplicatePhoneException(req.phone());
        });
        repository.findByDocumentNumber(req.documentNumber()).ifPresent(x -> {
            throw new DuplicateDocumentNumberException(req.documentNumber());
        });
        Beneficiary b = new Beneficiary();
        apply(b, req);
        repository.persist(b);
        return b;
    }

    @Transactional
    public Beneficiary update(Long id, BeneficiaryRequest req) {
        Beneficiary b = get(id);
        repository.findByPhone(req.phone())
                .filter(other -> !other.id.equals(id))
                .ifPresent(x -> {
                    throw new DuplicatePhoneException(req.phone());
                });
        apply(b, req);   // entidad "managed": el cambio se persiste al commit (dirty checking)
        return b;
    }


    @Transactional
    public void delete(Long id) {
        Beneficiary b = repository.findById(id);
        if (b == null) {
          throw new BeneficiaryNotFoundException(id);
        }
        b.active= false;
    }



    private void apply(Beneficiary b, BeneficiaryRequest req) {
        b.fullName = req.fullName();
        b.phone = req.phone();
        b.alias = req.alias();
        b.documentNumber= req.documentNumber();
    }
}
