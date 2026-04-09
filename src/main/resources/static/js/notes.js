function toggleDropdown(button) {
    const dropdown = button.closest('.action-dropdown');
    const menu = dropdown.querySelector('.dropdown-menu');
    const isOpen = menu.classList.contains('open');
    
    closeAllDropdowns();
    
    if (!isOpen) {
        menu.classList.add('open');
    }
}

function closeAllDropdowns() {
    document.querySelectorAll('.dropdown-menu.open').forEach(menu => {
        menu.classList.remove('open');
    });
}

document.addEventListener('click', function(e) {
    if (!e.target.closest('.action-dropdown')) {
        closeAllDropdowns();
    }
});

document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') {
        closeAllDropdowns();
        closeNotesModal();
    }
});

function openNotesModal(link) {
    const id = link.getAttribute('data-id');
    const name = link.getAttribute('data-name');
    const approach = link.getAttribute('data-approach');
    const code = link.getAttribute('data-code');
    
    document.getElementById('notesModalTitle').textContent = 'Solution: ' + name;
    document.getElementById('approachCode').textContent = approach || '';
    document.getElementById('codeBlock').textContent = code || '';
    
    const approachTab = document.querySelector('.notes-tab[data-tab="approach"]');
    const codeTab = document.querySelector('.notes-tab[data-tab="code"]');
    const approachContent = document.getElementById('approachContent');
    const codeContent = document.getElementById('codeContent');
    
    approachTab.style.display = approach ? '' : 'none';
    codeTab.style.display = code ? '' : 'none';
    
    if (approach && code) {
        switchNotesTab('approach');
    } else if (approach) {
        switchNotesTab('approach');
    } else if (code) {
        switchNotesTab('code');
    }
    
    document.getElementById('notesModal').classList.add('active');
    closeAllDropdowns();
}

function closeNotesModal() {
    document.getElementById('notesModal').classList.remove('active');
}

function switchNotesTab(tab) {
    const tabs = document.querySelectorAll('.notes-tab');
    const contents = document.querySelectorAll('.notes-content');
    
    tabs.forEach(t => t.classList.remove('active'));
    contents.forEach(c => c.classList.remove('active'));
    
    document.querySelector(`.notes-tab[data-tab="${tab}"]`).classList.add('active');
    document.getElementById(`${tab}Content`).classList.add('active');
}

document.addEventListener('click', function(e) {
    if (e.target.classList.contains('modal-overlay')) {
        closeNotesModal();
    }
});

document.addEventListener('DOMContentLoaded', function() {
    const approachEditor = document.getElementById('approachEditor');
    const codeEditor = document.getElementById('codeEditor');
    const approachPreview = document.getElementById('approachPreview');
    const codePreview = document.getElementById('codePreview');
    
    if (approachEditor && approachPreview) {
        const updateApproachPreview = () => {
            approachPreview.innerHTML = marked.parse(approachEditor.value || '');
        };
        approachEditor.addEventListener('input', updateApproachPreview);
        updateApproachPreview();
    }
    
    if (codeEditor && codePreview) {
        const updateCodePreview = () => {
            codePreview.innerHTML = marked.parse(codeEditor.value || '');
        };
        codeEditor.addEventListener('input', updateCodePreview);
        updateCodePreview();
    }
    
    if (window.successMessage) {
        showToast(window.successMessage, 'success');
        window.successMessage = null;
    }
    
    if (window.errorMessage) {
        showToast(window.errorMessage, 'error');
        window.errorMessage = null;
    }
});

function copyToClipboard(text, element) {
    navigator.clipboard.writeText(text).then(() => {
        if (element) {
            element.classList.add('copied');
            const originalHtml = element.innerHTML;
            element.innerHTML = '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg> Copied';
            setTimeout(() => {
                element.classList.remove('copied');
                element.innerHTML = originalHtml;
            }, 2000);
        }
        showToast('Copied to clipboard!', 'success');
    }).catch(() => {
        showToast('Failed to copy', 'error');
    });
}

function copyApproach() {
    const text = document.getElementById('approachCode').textContent;
    const btn = document.querySelector('[data-copy="approach"]');
    copyToClipboard(text, btn);
}

function copyCode() {
    const text = document.getElementById('codeBlock').textContent;
    const btn = document.querySelector('[data-copy="code"]');
    copyToClipboard(text, btn);
}

function showToast(message, type = 'success') {
    const container = document.getElementById('toastContainer');
    if (!container) return;
    
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    
    const icon = type === 'success' 
        ? '<svg class="toast-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>'
        : '<svg class="toast-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>';
    
    toast.innerHTML = `
        ${icon}
        <span class="toast-message">${message}</span>
        <button type="button" class="toast-close" onclick="hideToast(this)">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="18" y1="6" x2="6" y2="18"/>
                <line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
        </button>
    `;
    
    container.appendChild(toast);
    
    setTimeout(() => {
        if (toast.parentNode) {
            hideToast(toast.querySelector('.toast-close'));
        }
    }, 4000);
}

function hideToast(button) {
    const toast = button.closest('.toast');
    if (toast) {
        toast.classList.add('toast-exit');
        setTimeout(() => {
            if (toast.parentNode) {
                toast.parentNode.removeChild(toast);
            }
        }, 300);
    }
}
