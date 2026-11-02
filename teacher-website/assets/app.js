/* SmartAttend Teacher Portal | Local DEMO ONLY.
   No Firebase authentication, database writes, or verified BLE data occur here.
   Production integrations must be implemented server-side with verified roles. */
(() => {
  'use strict';
  const D = window.SmartAttendDemo;
  const $ = (q, root=document) => root.querySelector(q);
  const $$ = (q, root=document) => [...root.querySelectorAll(q)];
  const safe = (v) => String(v ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const fmtDate = (d) => { const value = new Date(`${d}T12:00:00`); return isNaN(+value) ? d : value.toLocaleDateString('en-IN',{day:'2-digit',month:'short',year:'numeric'}); };
  const pct = (s) => Math.round((s.attended / Math.max(s.total, 1)) * 100);
  const KEY = 'smartattend_teacher_demo_v2';
  const fallback = () => ({session:null,corrections:{},notices:[],threshold:75});
  let state;
  try { state = {...fallback(), ...(JSON.parse(localStorage.getItem(KEY) || 'null') || {})}; }
  catch { state = fallback(); }
  const save = () => { try { localStorage.setItem(KEY, JSON.stringify(state)); } catch { toast('Local browser storage is unavailable. Changes may not persist.'); } };
  const tag = (v) => `<span class="status ${v === 'Present'||v === 'Approved' ? 'ok':v === 'Absent'||v === 'Rejected'?'danger':'warn'}">${safe(v)}</span>`;
  const initials = (s) => s.trim().split(/\s+/).slice(0,2).map(x=>x[0]?.toUpperCase()).join('');
  function toast(msg){const n=$('#toast');if(!n)return;n.textContent=msg;n.hidden=false;clearTimeout(toast.timeout);toast.timeout=setTimeout(()=>n.hidden=true,4600);}
  function bindGlobal(){
    const menu=$('#menuToggle'), side=$('#sidebar');
    menu?.addEventListener('click',()=>{side.classList.toggle('open');menu.setAttribute('aria-expanded',String(side.classList.contains('open')));});
    $('#sidebarBackdrop')?.addEventListener('click',()=>side.classList.remove('open'));
    $$('[data-demo-reset]').forEach(n=>n.addEventListener('click',()=>{if(confirm('Reset ONLY locally stored demo data? This will not affect Firebase.')){state=fallback();save();location.reload();}}));
    $('#demoDate')?.replaceChildren(document.createTextNode(new Date().toLocaleDateString('en-IN',{weekday:'long',day:'numeric',month:'short',year:'numeric'})));
  }
  const tableStudents=(list)=>list.map(s=>`<tr><td><strong>${safe(s.id)}</strong></td><td><div class="identity"><span class="avatar small">${initials(s.name)}</span><span>${safe(s.name)}</span></div></td><td>${safe(s.batch)}</td><td>${s.attended}/${s.total}</td><td><div class="meter"><span style="width:${pct(s)}%"></span></div><small>${pct(s)}%</small></td><td>${pct(s)<state.threshold?tag('Below target'):tag('On track')}</td></tr>`).join('');
  function dashboard(){
    const all=D.students, avg=Math.round(all.reduce((sum,s)=>sum+pct(s),0)/all.length), low=all.filter(s=>pct(s)<state.threshold).length;
    $('#studentCount').textContent=all.length;$('#averageAttendance').textContent=avg+'%';$('#atRiskCount').textContent=low;
    $('#scheduledClasses').textContent=D.timetable.length;
    const tbody=$('#dashboardStudents');const filter=$('#batchSelect'), rate=$('#attendanceFilter');
    function update(){let rows=all.filter(s=>(!filter.value||s.batch===filter.value)&&(!rate.value||pct(s)<Number(rate.value)));
      tbody.innerHTML=rows.length?tableStudents(rows):'<tr><td colspan="6" class="empty-cell">No demo students match this filter.</td></tr>';
      $('#filteredCount').textContent=`${rows.length} demo students`;
    }
    filter?.addEventListener('change',update);rate?.addEventListener('change',update);update();
    $('#todaySchedule').innerHTML=D.timetable.slice(0,3).map(s=>`<div class="schedule-item"><span class="time-chip">${safe(s.start)}</span><div><strong>${safe(s.code)} · ${safe(s.type)}</strong><small>${safe(s.batch)} · Room ${safe(s.room)}</small></div><span class="pill muted">Sample</span></div>`).join('');
  }
  function students(){const search=$('#studentSearch'), batch=$('#studentBatch'), risk=$('#studentRisk'), tbody=$('#studentsTable');
    function update(){let q=search.value.trim().toLowerCase();let rows=D.students.filter(s=>[s.name,s.id,s.batch].some(x=>x.toLowerCase().includes(q))&&(!batch.value||s.batch===batch.value)&&(!risk.checked||pct(s)<state.threshold));
      tbody.innerHTML=rows.length?tableStudents(rows):'<tr><td colspan="6" class="empty-cell">No matching demo students.</td></tr>';
      $('#studentsResult').textContent=`Showing ${rows.length} of ${D.students.length} demo students`;
    }
    [search,batch,risk].forEach(el=>el?.addEventListener(el.type==='checkbox'?'change':'input',update));update();
  }
  function timetable(){const day=$('#daySelect'), tbody=$('#timetableTable');const dayNames=['Sunday','Monday','Tuesday','Wednesday','Thursday','Friday','Saturday'];
    day.value=dayNames[new Date().getDay()]||'Monday';
    function update(){const rows=D.timetable.filter(s=>!day.value||s.day===day.value);
      tbody.innerHTML=rows.length?rows.map(s=>`<tr><td><strong>${safe(s.day)}</strong></td><td>${safe(s.start)}–${safe(s.end)}</td><td><strong>${safe(s.subject)}</strong><small>${safe(s.code)}</small></td><td>${safe(s.batch)}</td><td>${safe(s.room)}</td><td>${tag(s.type==='Lab'?'Lab':'Lecture')}</td></tr>`).join(''):'<tr><td class="empty-cell" colspan="6">No sample classes on this day.</td></tr>';
    }
    day.addEventListener('change',update);update();
  }
  function live(){const btnStart=$('#startSession'),btnEnd=$('#endSession'),banner=$('#sessionBanner'),rows=$('#presenceTable'),start=$('#sessionStart');
    function paint(){const active=!!state.session?.active;btnStart.disabled=active;btnEnd.disabled=!active;
      banner.textContent=active?`LOCAL DEMO SESSION ACTIVE · ${state.session.subject} / ${state.session.batch}`:'No active local demo session';
      banner.className=`session-banner ${active?'active':''}`;
      start.textContent=active?new Date(state.session.startedAt).toLocaleString('en-IN'):'—';
      rows.innerHTML=active?D.students.filter(s=>s.batch===state.session.batch).map(s=>`<tr><td>${safe(s.id)}</td><td>${safe(s.name)}</td><td>${tag('Not detected')}</td><td>—</td><td>—</td><td>${tag('NOT VERIFIED')}</td></tr>`).join(''):'<tr><td colspan="6" class="empty-cell">Start a local demo session to preview the table. No Bluetooth scanning occurs.</td></tr>';
    }
    btnStart.addEventListener('click',()=>{const subject=$('#sessionSubject').value,batch=$('#sessionBatch').value,room=$('#sessionRoom').value.trim();if(!room){toast('Enter a room identifier.');return;}state.session={active:true,subject,batch,room,startedAt:new Date().toISOString()};save();paint();toast('Local demo session started. Not synchronized with students.');});
    btnEnd.addEventListener('click',()=>{if(!state.session?.active)return;if(confirm('End this LOCAL DEMO session? No official attendance will be recorded.')){state.session=null;save();paint();toast('Demo session ended. No official attendance was changed.');}});
    paint();
  }
  function corrections(){const tbody=$('#correctionsTable'), counter=$('#pendingCorrections');
    function draw(){const rows=D.corrections.map(x=>({...x,status:state.corrections[x.id]||x.status}));
      counter.textContent=rows.filter(x=>x.status==='Pending').length;
      tbody.innerHTML=rows.map(x=>`<tr><td><strong>${safe(x.student)}</strong><small>${safe(x.roll)}</small></td><td>${safe(x.subject)}<small>${fmtDate(x.date)}</small></td><td>${safe(x.current)} → ${safe(x.expected)}</td><td class="reason-cell">${safe(x.reason)}</td><td>${tag(x.status)}</td><td>${x.status==='Pending'?`<div class="inline-actions"><button type="button" class="mini-btn" data-decision="Approved" data-id="${safe(x.id)}">Approve demo</button><button type="button" class="mini-btn alt" data-decision="Rejected" data-id="${safe(x.id)}">Reject demo</button></div>`:'<span class="muted-text">Demo decision</span>'}</td></tr>`).join('');
      $$('[data-decision]').forEach(btn=>btn.addEventListener('click',()=>{const id=btn.dataset.id,decision=btn.dataset.decision;if(!confirm(`Simulate ${decision.toLowerCase()}? This does NOT update any actual attendance or Firebase record.`))return;state.corrections[id]=decision;save();draw();toast(`Demo request ${decision.toLowerCase()}. Firebase remains unchanged.`);}));
    }
    draw();
  }
  function notices(){const list=$('#noticeList'),form=$('#noticeForm');
    function draw(){const notices=[...state.notices,...D.notices];$('#noticeCount').textContent=notices.length;
      list.innerHTML=notices.length?notices.map(x=>`<article class="notice-card"><div class="row between wrap"><span class="pill muted">${safe(x.subject)} · ${safe(x.batch)}</span><span class="tiny">Due ${fmtDate(x.due)}</span></div><h3>${safe(x.title)}</h3><p>${safe(x.message)}</p><small>${x.id.startsWith('NOTICE-DEMO')?'Seeded example':'Saved locally in this browser'}</small></article>`).join(''):'<p>No notices yet.</p>';
    }
    form.addEventListener('submit',e=>{e.preventDefault();if(!form.reportValidity())return;const data=new FormData(form),notice={id:'LOCAL-'+Date.now(),title:data.get('title').trim(),message:data.get('message').trim(),subject:data.get('subject'),batch:data.get('batch'),due:data.get('due')};if(!notice.title||!notice.message){toast('Please enter a title and message.');return;}state.notices.unshift(notice);save();draw();form.reset();toast('Notice saved ONLY in local demo storage. Students have not been notified.');});draw();
  }
  function reports(){const threshold=state.threshold;
    $('#reportTotal').textContent=D.students.length;$('#reportAverage').textContent=Math.round(D.students.reduce((n,s)=>n+pct(s),0)/D.students.length)+'%';
    $('#reportRisk').textContent=D.students.filter(s=>pct(s)<threshold).length;
    $('#reportBars').innerHTML=D.students.map(s=>`<div class="bar-group"><span title="${safe(s.name)}">${safe(s.name)}</span><div class="bar-track"><div class="bar-fill ${pct(s)<threshold?'low':''}" style="width:${pct(s)}%"></div></div><strong>${pct(s)}%</strong></div>`).join('');
    $('#reportTable').innerHTML=tableStudents(D.students);
    $('#exportCsv').addEventListener('click',()=>{const lines=[['DEMO RECORD - NOT REAL ATTENDANCE'],['Roll','Name','Batch','Attended','Total','Percentage'],...D.students.map(s=>[s.id,s.name,s.batch,s.attended,s.total,pct(s)])];
      const content=lines.map(row=>row.map(v=>{let s=String(v);if(/^[=+\-@\t\r]/.test(s))s="'"+s;return '"'+s.replaceAll('"','""')+'"';}).join(',')).join('\r\n');
      const url=URL.createObjectURL(new Blob([content],{type:'text/csv;charset=utf-8'}));const a=document.createElement('a');a.href=url;a.download='SmartAttend_DEMO_report.csv';a.click();URL.revokeObjectURL(url);toast('Downloaded DEMO data only.');
    });
  }
  function settings(){const slider=$('#threshold'),display=$('#thresholdValue');slider.value=state.threshold;
    display.textContent=state.threshold+'%';slider.addEventListener('input',()=>display.textContent=slider.value+'%');
    $('#saveSettings').addEventListener('click',()=>{state.threshold=Number(slider.value);save();toast('Threshold saved for this browser DEMO only.');});
  }
  function login(){const form=$('#teacherLogin');form?.addEventListener('submit',e=>{e.preventDefault();$('#loginStatus').textContent='Firebase teacher authentication has not been configured in this starter. No credentials were sent or stored.';$('#loginStatus').classList.remove('hidden');});}
  bindGlobal();
  const page=document.body.dataset.page;
  ({dashboard,students,timetable,live,corrections,notices,reports,settings,login}[page]||(()=>{}))();
})();
