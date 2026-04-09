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
});
