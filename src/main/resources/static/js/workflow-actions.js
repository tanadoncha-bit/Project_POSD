window.workflowApi = async function(url, options={}) {
 const header=document.querySelector('meta[name="_csrf_header"]')?.content;
 const token=document.querySelector('meta[name="_csrf"]')?.content;
 const response=await fetch(url,{...options,headers:{"Content-Type":"application/json",...(header&&token?{[header]:token}:{}),...(options.headers||{})}});
 const text=await response.text(); let body={}; try { body=text?JSON.parse(text):{}; } catch {}
 if(!response.ok) throw new Error(body.message||"Unable to complete this action."); return body;
};
window.workflowForm = function(title, description, label, submit) {
 return new Promise(resolve => {
  const dialog=document.createElement('dialog'); dialog.className='workflow-dialog';
  const form=document.createElement('form'); const heading=document.createElement('h2'); heading.textContent=title;
  const intro=document.createElement('p'); intro.textContent=description;
  const field=document.createElement('label'); field.textContent=label;
  const input=document.createElement('textarea'); input.required=true; input.maxLength=500; field.append(input);
  const error=document.createElement('p'); error.setAttribute('role','alert');
  const footer=document.createElement('footer'); const cancel=document.createElement('button'); cancel.type='button'; cancel.textContent='Close'; cancel.onclick=()=>dialog.close();
  const confirm=document.createElement('button'); confirm.type='submit'; confirm.textContent='Confirm'; footer.append(cancel,confirm);
  form.append(heading,intro,field,error,footer); dialog.append(form); document.body.append(dialog);
  const previous=document.activeElement; let completed=false;
  dialog.addEventListener('close',()=>{dialog.remove();previous?.focus();resolve(completed);},{once:true});
  form.onsubmit=async event=>{event.preventDefault();confirm.disabled=true;try { await submit(input.value.trim());completed=true;dialog.close(); } catch(e) {error.textContent=e.message;} finally {confirm.disabled=false;} };
  dialog.showModal(); input.focus();
 });
};
window.workflowAction = async function(kind,id) {
 if(kind==='settlement') {
  const info=await workflowApi(`/api/v1/borrow-requests/${id}/settlement`);
  if(info.paid) {toast.info('Payment has already been recorded.');return false;}
  if(!info.finalized || Number(info.total)<=0) {toast.info('No finalized outstanding payment.');return false;}
  return workflowForm('Record payment',`Confirm that ${Number(info.total).toFixed(2)} THB has actually been received. This does not charge a bank account.`,'Receipt / payment reference',reference=>workflowApi(`/api/v1/borrow-requests/${id}/settlement`,{method:'POST',body:JSON.stringify({reference,amount:info.total})}));
 }
 const repair=kind==='repair';
 return workflowForm(repair?'Complete repair':'Reject request',repair?'Confirm the equipment has been repaired and is ready to borrow.':'The borrower will see this reason.','Reason / details',reason=>workflowApi(repair?`/api/v1/equipment/${id}/repair`:`/api/v1/borrow-requests/${id}/reject`,{method:'POST',body:JSON.stringify({reason})}));
};
