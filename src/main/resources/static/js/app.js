document.addEventListener('DOMContentLoaded', () => {
    initDeleteOfferModal();
    initSubmitConfirmations();
});

function initDeleteOfferModal() {
    const deleteOfferModal = document.getElementById('deleteOfferModal');
    const deleteOfferMessage = document.getElementById('deleteOfferMessage');
    const cancelDeleteOffer = document.getElementById('cancelDeleteOffer');
    const confirmDeleteOffer = document.getElementById('confirmDeleteOffer');
    const deleteButtons = document.querySelectorAll('.delete-offer-trigger');

    if (!deleteOfferModal || !deleteOfferMessage || !cancelDeleteOffer || !confirmDeleteOffer || deleteButtons.length === 0) {
        return;
    }

    let selectedDeleteFormId = null;

    const closeDeleteModal = () => {
        deleteOfferModal.classList.remove('is-visible');
        deleteOfferModal.setAttribute('aria-hidden', 'true');
        selectedDeleteFormId = null;
    };

    deleteButtons.forEach((button) => {
        button.addEventListener('click', () => {
            selectedDeleteFormId = button.dataset.formId;
            deleteOfferMessage.textContent = `Please confirm that you want to delete "${button.dataset.offerTitle}". This offer will be removed from your public listings.`;
            deleteOfferModal.classList.add('is-visible');
            deleteOfferModal.setAttribute('aria-hidden', 'false');
            cancelDeleteOffer.focus();
        });
    });

    cancelDeleteOffer.addEventListener('click', closeDeleteModal);

    deleteOfferModal.addEventListener('click', (event) => {
        if (event.target === deleteOfferModal) {
            closeDeleteModal();
        }
    });

    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape' && deleteOfferModal.classList.contains('is-visible')) {
            closeDeleteModal();
        }
    });

    confirmDeleteOffer.addEventListener('click', () => {
        if (selectedDeleteFormId) {
            document.getElementById(selectedDeleteFormId).submit();
        }
    });
}

function initSubmitConfirmations() {
    document.querySelectorAll('form').forEach((form) => {
        form.addEventListener('submit', (event) => {
            const submitter = event.submitter;

            if (submitter?.dataset.confirmMessage && !confirm(submitter.dataset.confirmMessage)) {
                event.preventDefault();
            }
        });
    });
}
