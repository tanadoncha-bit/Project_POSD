(() => {
    if (window.leadItPickersInitialized) return;
    window.leadItPickersInitialized = true;
    function pickerIcon(kind) {
        const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        svg.setAttribute('viewBox', '0 0 24 24');
        svg.setAttribute('width', '18'); svg.setAttribute('height', '18');
        svg.setAttribute('fill', 'none'); svg.setAttribute('stroke', 'currentColor');
        svg.setAttribute('stroke-width', '1.7'); svg.setAttribute('stroke-linecap', 'round');
        svg.setAttribute('stroke-linejoin', 'round'); svg.setAttribute('aria-hidden', 'true');
        const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
        path.setAttribute('d', kind === 'calendar' ? 'M8 3v4m8-4v4M4 10h16M6 5h12a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2Z' : kind === 'previous' ? 'm14 6-6 6 6 6' : 'm10 6 6 6-6 6');
        svg.append(path); return svg;
    }
    let sequence = 0;
    const initialized = new WeakSet();
    const iso = date => `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`;
    function enhance(input) {
        if (initialized.has(input)) return;
        initialized.add(input);
        const lifecycle = new AbortController();
        const signal = lifecycle.signal;
        const dateMode = input.type === 'date';
        const wrapper = document.createElement('span'); wrapper.className = 'form-picker';
        input.before(wrapper); wrapper.append(input); input.classList.add('picker-native'); input.tabIndex = -1;
        const trigger = document.createElement('button'); trigger.type = 'button'; trigger.className = 'picker-trigger';
        trigger.classList.toggle('picker-date-trigger', dateMode);
        const panel = document.createElement('div'); panel.className = 'picker-panel'; panel.id = `picker-${++sequence}`; panel.setAttribute('popover','auto');
        trigger.setAttribute('aria-expanded','false'); trigger.setAttribute('aria-controls',panel.id);
        trigger.setAttribute('aria-haspopup', dateMode ? 'dialog' : 'listbox');
        panel.setAttribute('role', dateMode ? 'dialog' : 'listbox');
        const label = input.closest('label');
        const externalLabel = input.id ? document.querySelector(`label[for="${input.id}"]`) : null;
        const name = (externalLabel?.textContent || label?.firstChild?.textContent || (dateMode ? 'Return date' : 'Select option')).trim();
        panel.setAttribute('aria-label',name);
        wrapper.append(trigger,panel);
        let month = new Date();
        function sync() {
            trigger.textContent = dateMode ? (input.value ? new Date(input.value+'T12:00:00').toLocaleDateString('en-GB',{day:'numeric',month:'short',year:'numeric'}) : 'Choose date') : input.selectedOptions[0]?.textContent || 'Select option';
            trigger.setAttribute('aria-label', `${name}: ${trigger.textContent}`);
            if (dateMode) trigger.append(pickerIcon('calendar'));
            trigger.disabled = input.disabled || input.readOnly;
        }
        function choose(value) { input.value = value; input.dispatchEvent(new Event('input',{bubbles:true})); input.dispatchEvent(new Event('change',{bubbles:true})); sync(); panel.hidePopover(); trigger.focus(); }
        function button(text, action, disabled = false) { const b=document.createElement('button'); b.type='button'; b.textContent=text; b.disabled=disabled; b.addEventListener('click',action); return b; }
        function render() {
            panel.replaceChildren();
            if (!dateMode) {
                Array.from(input.options).forEach(option => {
                    if (!option.value && input.required) return;
                    const b=button(option.textContent,()=>choose(option.value),option.disabled);
                    b.setAttribute('role','option'); b.setAttribute('aria-selected',String(option.selected)); panel.append(b);
                });
            } else {
                const head=document.createElement('div'); head.className='picker-calendar-header';
                const previous=button('',()=>{month.setMonth(month.getMonth()-1);render();panel.querySelector('[aria-label="Previous month"]').focus();}); previous.setAttribute('aria-label','Previous month'); previous.replaceChildren(pickerIcon('previous'));
                const next=button('',()=>{month.setMonth(month.getMonth()+1);render();panel.querySelector('[aria-label="Next month"]').focus();}); next.setAttribute('aria-label','Next month'); next.replaceChildren(pickerIcon('next'));
                const title=document.createElement('strong'); title.textContent=month.toLocaleDateString('en-GB',{month:'long',year:'numeric'});
                head.append(previous,title,next); panel.append(head);
                const grid=document.createElement('div'); grid.className='picker-calendar-grid';
                ['Su','Mo','Tu','We','Th','Fr','Sa'].forEach(day=>{const t=document.createElement('span');t.textContent=day;grid.append(t);});
                const first=new Date(month.getFullYear(),month.getMonth(),1);
                for(let i=0;i<first.getDay();i++) grid.append(document.createElement('span'));
                const total=new Date(month.getFullYear(),month.getMonth()+1,0).getDate();
                for(let i=1;i<=total;i++) {
                    const value=iso(new Date(month.getFullYear(),month.getMonth(),i));
                    const b=button(String(i),()=>choose(value),Boolean(input.min && value<input.min || input.max && value>input.max));
                    b.setAttribute('aria-label',value); b.classList.toggle('selected',value===input.value);
                    if(value===iso(new Date())) b.setAttribute('aria-current','date'); grid.append(b);
                }
                panel.append(grid);
            }
        }
        function position() {
            const r=trigger.getBoundingClientRect();
            const width=Math.min(dateMode?300:Math.max(r.width,180),innerWidth-24);
            panel.style.width=width+'px'; panel.style.maxHeight=Math.min(340,innerHeight-24)+'px';
            panel.style.left=Math.max(12,Math.min(r.left,innerWidth-width-12))+'px';
            const height=panel.getBoundingClientRect().height;
            panel.style.top=Math.max(12,r.bottom+height+12<innerHeight?r.bottom+10:r.top-height-10)+'px';
        }
        trigger.addEventListener('click',()=>{
            if(panel.matches(':popover-open')) {panel.hidePopover();return;}
            month=input.value&&dateMode?new Date(input.value+'T12:00:00'):new Date(); month.setDate(1);
            render(); panel.showPopover(); position();
            (panel.querySelector('[aria-selected="true"]:not(:disabled), .selected:not(:disabled)') || panel.querySelector('button:not(:disabled)'))?.focus();
        });
        panel.addEventListener('toggle',()=>trigger.setAttribute('aria-expanded',String(panel.matches(':popover-open'))));
        panel.addEventListener('keydown',event=>{
            if(event.key==='Escape'){event.preventDefault();event.stopPropagation();panel.hidePopover();trigger.focus();return;}
            if(event.key==='Tab'){panel.hidePopover();trigger.focus();return;}
            const buttons=Array.from(panel.querySelectorAll('button:not(:disabled)')); const at=buttons.indexOf(document.activeElement);
            let step={ArrowDown:dateMode?7:1,ArrowUp:dateMode?-7:-1,ArrowRight:1,ArrowLeft:-1}[event.key];
            if(step!==undefined){event.preventDefault();buttons[Math.max(0,Math.min(buttons.length-1,at+step))]?.focus();}
            if(event.key==='Home'||event.key==='End'){event.preventDefault();buttons[event.key==='Home'?0:buttons.length-1]?.focus();}
        });
        input.addEventListener('change',sync); input.addEventListener('invalid',()=>trigger.focus());
        trigger.addEventListener('focus',sync);
        const observer = new MutationObserver(sync); observer.observe(input,{childList:true,subtree:true,attributes:true});
        input.closest('dialog')?.addEventListener('close', () => { lifecycle.abort(); observer.disconnect(); }, {once:true});
        input.form?.addEventListener('reset',()=>setTimeout(sync,0));
        document.addEventListener('picker:sync',sync,{signal});
        window.addEventListener('resize',()=>{if(panel.matches(':popover-open'))position();},{signal});
        document.addEventListener('scroll',()=>{if(panel.matches(':popover-open'))position();},{capture:true,signal});
        sync();
    }
    function scan() {
        if (!('showPopover' in HTMLElement.prototype)) return;
        document.querySelectorAll('[data-admin-status-filter], #admin-category, #admin-edit-equipment-form select, #borrow-due-date').forEach(enhance);
    }
    if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',scan);else scan();
    new MutationObserver(records=>{if(records.some(r=>Array.from(r.addedNodes).some(n=>n.nodeType===1&&!n.closest?.('.form-picker'))))scan();}).observe(document.documentElement,{childList:true,subtree:true});
})();
