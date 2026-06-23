document.addEventListener('DOMContentLoaded', () => {
    const confirmationModal = createConfirmationModal();
    initDeleteOfferModal(confirmationModal);
    initSubmitConfirmations(confirmationModal);
});

function createConfirmationModal() {
    let modal = document.getElementById('confirmationModal');

    if (!modal) {
        modal = document.createElement('div');
        modal.id = 'confirmationModal';
        modal.className = 'modal-backdrop';
        modal.setAttribute('aria-hidden', 'true');
        modal.innerHTML = `
            <div class="confirmation-modal" role="dialog" aria-modal="true" aria-labelledby="confirmationModalTitle">
                <h2 id="confirmationModalTitle">Please confirm</h2>
                <p id="confirmationModalMessage"></p>
                <div class="modal-actions">
                    <button type="button" class="btn btn-secondary" id="confirmationModalCancel">Cancel</button>
                    <button type="button" class="btn btn-danger" id="confirmationModalConfirm">Confirm</button>
                </div>
            </div>
        `;
        document.body.appendChild(modal);
    }

    const title = modal.querySelector('#confirmationModalTitle');
    const message = modal.querySelector('#confirmationModalMessage');
    const cancelButton = modal.querySelector('#confirmationModalCancel');
    const confirmButton = modal.querySelector('#confirmationModalConfirm');

    let onConfirm = null;

    const close = () => {
        modal.classList.remove('is-visible');
        modal.setAttribute('aria-hidden', 'true');
        onConfirm = null;
    };

    cancelButton.addEventListener('click', close);

    modal.addEventListener('click', (event) => {
        if (event.target === modal) {
            close();
        }
    });

    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape' && modal.classList.contains('is-visible')) {
            close();
        }
    });

    confirmButton.addEventListener('click', () => {
        if (onConfirm) {
            onConfirm();
        }
        close();
    });

    return {
        open(options) {
            title.textContent = options.title || 'Please confirm';
            message.textContent = options.message;
            cancelButton.textContent = options.cancelLabel || 'Cancel';
            confirmButton.textContent = options.confirmLabel || 'Confirm';
            onConfirm = options.onConfirm;

            modal.classList.add('is-visible');
            modal.setAttribute('aria-hidden', 'false');
            cancelButton.focus();
        }
    };
}

function initDeleteOfferModal(confirmationModal) {
    document.querySelectorAll('.delete-offer-trigger').forEach((button) => {
        button.addEventListener('click', () => {
            confirmationModal.open({
                title: button.dataset.confirmTitle,
                message: `Please confirm that you want to delete "${button.dataset.offerTitle}". This offer will be removed from your public listings.`,
                cancelLabel: button.dataset.confirmCancelLabel,
                confirmLabel: button.dataset.confirmConfirmLabel,
                onConfirm: () => {
                    const form = document.getElementById(button.dataset.formId);
                    if (form) {
                        form.submit();
                    }
                }
            });
        });
    });
}

function initSubmitConfirmations(confirmationModal) {
    document.querySelectorAll('form').forEach((form) => {
        form.addEventListener('submit', (event) => {
            const submitter = event.submitter;

            if (!submitter?.dataset.confirmMessage) {
                return;
            }

            event.preventDefault();

            confirmationModal.open({
                title: submitter.dataset.confirmTitle || 'Please confirm',
                message: submitter.dataset.confirmMessage,
                cancelLabel: submitter.dataset.confirmCancelLabel || 'Cancel',
                confirmLabel: submitter.dataset.confirmConfirmLabel || submitter.textContent.trim(),
                onConfirm: () => form.submit()
            });
        });
    });
}
