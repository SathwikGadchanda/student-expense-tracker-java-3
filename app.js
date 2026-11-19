"use strict";
const groups = {
  Expense:["Food","Transportation","Education","Shopping","Entertainment","Accommodation","Health","Other"],
  Income:["Pocket Money","Scholarship","Part-time Work","Freelance","Gift","Other"]
};
const colors = ["#185843","#6b9e72","#295d73","#bd9054","#7b688f","#b96849","#467e85","#83926a"];
const cash = new Intl.NumberFormat("en-IN",{style:"currency",currency:"INR"});
const money = paise => cash.format(paise/100);
const byId = id => document.getElementById(id);
const pad = value => String(value).padStart(2,"0");
const dateKey = date => [date.getFullYear(),pad(date.getMonth()+1),pad(date.getDate())].join("-");
const today = dateKey(new Date());
const thisMonth = today.slice(0,7);
const monthLabel = ym => new Intl.DateTimeFormat("en-IN",{month:"long",year:"numeric"}).format(new Date(ym+"-15T12:00:00"));
const escapeText = value => String(value).replaceAll("&","&amp;").replaceAll("<","&lt;").replaceAll(">","&gt;").replaceAll('"',"&quot;").replaceAll("'","&#39;");
function paise(raw){
  if(!/^\d+(\.\d{1,2})?$/.test(raw.trim()))return null;
  const [rupees,decimals=""] = raw.trim().split(".");
  const result = Number(rupees)*100 + Number(decimals.padEnd(2,"0"));
  return Number.isSafeInteger(result)&&result>0&&result<=100000000 ? result : null;
}
function seed(){
  const prev = new Date(thisMonth+"-01T12:00:00");prev.setMonth(prev.getMonth()-1);
  const last = dateKey(prev).slice(0,7);
  const values=[
    ["Income","Pocket Money",8000,1,"Monthly allowance"],["Income","Part-time Work",1500,3,"Weekend tutoring"],
    ["Expense","Food",120,3,"Lunch at college canteen"],["Expense","Transportation",450,4,"Bus pass"],
    ["Expense","Education",850,5,"Books and lab supplies"],["Expense","Food",380,6,"Weekly groceries"],
    ["Expense","Shopping",600,7,"Backpack"],["Expense","Entertainment",250,8,"Movie with friends"],
    ["Expense","Other",100,9,"Laundry"],["Expense","Food",200,10,"Dinner"],
    ["Income","Pocket Money",5000,1,"Previous month allowance",last],["Expense","Food",2100,5,"Previous month meals",last]
  ];
  return {version:1,budgets:{[thisMonth]:350000},rows:values.map(([kind,category,rupees,day,description,month],i)=>
    ({id:i+1,kind,category,amount:rupees*100,date:(month||thisMonth)+"-"+pad(day),description}))};
}
const key="student-expense-tracker-demo-v1";
let data=seed(),storageMessage="";
try{const raw=localStorage.getItem(key);if(raw){const d=JSON.parse(raw);if(d.version===1&&Array.isArray(d.rows)&&d.budgets)data=d;}}
catch{storageMessage="Browser storage is unavailable. Changes last for this visit.";}
let page="Overview", selectedMonth=thisMonth, editId=null, query="", typeFilter="", onlyMonth=false;
function write(message){
  try{localStorage.setItem(key,JSON.stringify(data));}catch{storageMessage="Your browser could not save this change. It will last for this visit.";}
  byId("status").textContent=storageMessage||message;render();
}
const sorted = rows => [...rows].sort((a,b)=>b.date.localeCompare(a.date)||b.id-a.id);
const sum = (rows,kind)=>rows.reduce((v,r)=>v+(r.kind===kind?r.amount:0),0);
function groupsBy(rows,getKey){
  const map = new Map();
  for(const row of rows){const k=getKey(row);map.set(k,(map.get(k)||0)+row.amount);}
  return [...map];
}
function snapshot(){
  const rows=data.rows.filter(r=>r.date.slice(0,7)===selectedMonth);
  return {rows,income:sum(rows,"Income"),spent:sum(rows,"Expense"),budget:data.budgets[selectedMonth]||0,
    categories:groupsBy(rows.filter(r=>r.kind==="Expense"),r=>r.category).sort((a,b)=>b[1]-a[1])};
}
function budgetState(spent,limit){
  if(!limit)return "No budget set";
  if(spent>limit)return "Budget exceeded";
  if(spent===limit)return "Budget reached";
  return spent*100>=limit*80?"80% warning":"Within budget";
}
function chart(s){
  if(!s.categories.length)return '<p class="empty">No expenses recorded for this month.</p>';
  let end=0;
  const wedges=s.categories.map(([cat,value],i)=>{const from=end;end+=100*value/s.spent;return colors[i]+" "+from+"% "+end+"%";});
  const legend=s.categories.map(([cat,value],i)=>'<div><i style="background:'+colors[i]+'"></i><span>'+escapeText(cat)+'</span><b>'+money(value)+'</b></div>').join("");
  return '<div class="chart"><div class="donut" aria-hidden="true" style="background:conic-gradient('+wedges.join(",")+')"><div><small>Spent</small><strong>'+new Intl.NumberFormat("en-IN",{maximumFractionDigits:2}).format(s.spent/100)+'</strong></div></div><div class="legend">'+legend+'</div></div>';
}
function budgetPanel(s){
  const state=budgetState(s.spent,s.budget), pct=s.budget?100*s.spent/s.budget:0;
  const label=!s.budget?"Set a budget":s.spent>s.budget?"Over budget":"Remaining";
  return '<article><div class="section-head"><h2>Monthly budget</h2><span class="flag '+(s.spent>s.budget&&s.budget?"over":pct>=80?"warn":"")+'">'+state+'</span></div><span class="big">'+(s.budget?money(Math.abs(s.budget-s.spent)):"—")+'</span><span class="meta">'+label+'</span><div class="bar"><span style="width:'+Math.min(100,pct)+'%"></span></div><p class="note">'+money(s.spent)+' spent'+(s.budget?' of '+money(s.budget):' this month')+'</p><button class="budget-actions" data-act="budget">'+(s.budget?"Edit budget":"Set monthly limit")+'</button></article>';
}
function table(rows,editable){
  if(!rows.length)return '<p class="empty">No matching transactions.</p>';
  const body=rows.map(r=>'<tr class="entry"><td>'+escapeText(r.date)+'</td><td class="desc">'+escapeText(r.description||"—")+'</td><td>'+escapeText(r.category)+'</td><td>'+escapeText(r.kind)+'</td><td class="amt '+(r.kind==="Income"?"positive":"")+'">'+(r.kind==="Income"?"+":"−")+money(r.amount)+'</td>'+(editable?'<td><button data-edit="'+r.id+'" aria-label="Edit transaction '+r.id+'">Edit</button></td>':'')+'</tr>').join("");
  return '<div class="scroll"><table><thead><tr><th>Date</th><th>Description</th><th>Category</th><th>Type</th><th class="amt">Amount</th>'+(editable?'<th>Edit</th>':'')+'</tr></thead><tbody>'+body+'</tbody></table></div>';
}
function totals(s){
  const income=sum(data.rows,"Income"),expenses=sum(data.rows,"Expense");
  return '<div class="cards">'+[["Current balance",income-expenses,"All recorded transactions"],["Total income",income,"All time"],["Monthly spending",s.spent,monthLabel(selectedMonth)]].map(([t,v,h])=>'<section class="card"><h2>'+t+'</h2><b>'+money(v)+'</b><small>'+h+'</small></section>').join("")+'</div>';
}
function updateLedger(){
  const filtered=sorted(data.rows.filter(r=>(!typeFilter||r.kind===typeFilter)&&(!onlyMonth||r.date.startsWith(selectedMonth))&&
    [r.description,r.category,r.date,r.kind].some(x=>String(x).toLowerCase().includes(query.trim().toLowerCase()))));
  byId("ledger-body").innerHTML=table(filtered,true);
}
function reportBlock(title,rows){
  return '<article><h2>'+title+'</h2>'+(rows.length?'<table><tbody>'+rows.map(([k,v])=>'<tr><td>'+escapeText(k)+'</td><td class="amt">'+money(v)+'</td></tr>').join("")+'</tbody></table>':'<p class="empty">No entries.</p>')+'</article>';
}
function weekStart(value){const date=new Date(value+"T12:00:00");date.setDate(date.getDate()-(date.getDay()+6)%7);return dateKey(date);}
function render(){
  const s=snapshot(),view=byId("view");byId("page-title").textContent=page;
  for(const btn of document.querySelectorAll("nav button")){if(btn.dataset.page===page)btn.setAttribute("aria-current","page");else btn.removeAttribute("aria-current");}
  if(page==="Overview"){
    view.innerHTML=totals(s)+'<div class="row"><article><div class="section-head"><h2>Spending by category</h2></div>'+chart(s)+'</article>'+budgetPanel(s)+'</div><article><div class="section-head"><h2>Recent transactions</h2><button class="secondary" data-page="Transactions">View all</button></div>'+table(sorted(data.rows).slice(0,4),false)+'</article>';
  } else if(page==="Transactions"){
    view.innerHTML='<article><div class="section-head"><h2>Transaction history</h2><button data-act="csv">Export all to CSV</button></div><div class="controls"><input id="search" type="search" aria-label="Search transactions" placeholder="Search description, category or date"><select id="filter" aria-label="Transaction type"><option value="">All types</option><option>Expense</option><option>Income</option></select><label class="check"><input type="checkbox" id="only-month"> Selected month only</label></div><div id="ledger-body"></div></article>';
    byId("search").value=query;byId("filter").value=typeFilter;byId("only-month").checked=onlyMonth;
    byId("search").oninput=e=>{query=e.target.value;updateLedger();};byId("filter").onchange=e=>{typeFilter=e.target.value;updateLedger();};byId("only-month").onchange=e=>{onlyMonth=e.target.checked;updateLedger();};updateLedger();
  } else if(page==="Budgets"){
    view.innerHTML='<div class="row">'+budgetPanel(s)+reportBlock("Saved monthly limits",Object.entries(data.budgets).sort((a,b)=>b[0].localeCompare(a[0])).map(([k,v])=>[monthLabel(k),v]))+'</div>';
  } else {
    const ex=s.rows.filter(r=>r.kind==="Expense"),asc=(a,b)=>a[0].localeCompare(b[0]);
    view.innerHTML='<div class="row">'+reportBlock("Income and expenses",[["Income",s.income],["Expenses",s.spent],["Net change",s.income-s.spent]])+'<article><div class="section-head"><h2>Spending by category</h2></div>'+chart(s)+'</article></div><div class="detail-grid">'+
      reportBlock("Daily expenses",groupsBy(ex,r=>r.date).sort(asc))+reportBlock("Week beginning Monday",groupsBy(ex,r=>weekStart(r.date)).sort(asc))+
      reportBlock("Monthly history",groupsBy(data.rows.filter(r=>r.kind==="Expense"),r=>r.date.slice(0,7)).sort(asc))+'</div>';
  }
}
function setCategories(selected){
  const options=groups[byId("kind").value];byId("category").replaceChildren(...options.map(cat=>new Option(cat,cat,false,cat===selected)));
}
function openEntry(id){
  const row=id==null?null:data.rows.find(r=>r.id===id);editId=row?.id??null;
  byId("entry-title").textContent=row?"Edit transaction":"Add transaction";byId("kind").value=row?.kind||"Expense";setCategories(row?.category);
  byId("date").value=row?.date||(selectedMonth===thisMonth?today:selectedMonth+"-01");byId("amount").value=row?(row.amount/100).toFixed(2):"";
  byId("description").value=row?.description||"";byId("remove").hidden=!row;byId("amount").setCustomValidity("");byId("entry").showModal();
}
function validAmount(input){
  const v=paise(input.value);input.setCustomValidity(v===null?"Enter INR 0.01 to 10,00,000.00, with up to two decimal places.":"");if(v===null)input.reportValidity();return v;
}
function csv(){
  const cell=value=>{let s=String(value);if(/^[\s]*[=+@-]/.test(s))s="'"+s;return '"'+s.replaceAll('"','""')+'"';};
  const csvRows=[["ID","Type","Amount (INR)","Category","Date","Description"],...sorted(data.rows).map(r=>[r.id,r.kind,(r.amount/100).toFixed(2),r.category,r.date,r.description])];
  const blob=new Blob([csvRows.map(r=>r.map(cell).join(",")).join("\r\n")+"\r\n"],{type:"text/csv;charset=utf-8"});
  const url=URL.createObjectURL(blob),a=document.createElement("a");a.href=url;a.download="student-transactions.csv";a.click();setTimeout(()=>URL.revokeObjectURL(url),1500);
}
byId("month").value=selectedMonth;byId("month").onchange=e=>{if(/^\d{4}-\d{2}$/.test(e.target.value)){selectedMonth=e.target.value;render();}else e.target.value=selectedMonth;};
document.addEventListener("click",e=>{
  const btn=e.target.closest("button");if(!btn)return;
  if(btn.dataset.page){page=btn.dataset.page;byId("status").textContent=storageMessage;render();return;}
  if(btn.dataset.edit){openEntry(Number(btn.dataset.edit));return;}
  switch(btn.dataset.act){
    case "add":openEntry(null);break;
    case "close":btn.closest("dialog").close();break;
    case "csv":csv();break;
    case "budget":byId("limit-month").textContent=monthLabel(selectedMonth);byId("limit-amount").value=data.budgets[selectedMonth]?(data.budgets[selectedMonth]/100).toFixed(2):"";byId("limit-amount").setCustomValidity("");byId("limit").showModal();break;
  }
});
byId("kind").onchange=()=>setCategories();for(const id of ["amount","limit-amount"])byId(id).oninput=e=>e.target.setCustomValidity("");
byId("entry-form").onsubmit=e=>{
  e.preventDefault();const amount=validAmount(byId("amount"));if(amount===null)return;
  const id=editId??data.rows.reduce((v,r)=>Math.max(v,r.id),0)+1;
  const row={id,kind:byId("kind").value,category:byId("category").value,date:byId("date").value,description:byId("description").value.trim(),amount};
  if(editId===null)data.rows.push(row);else data.rows=data.rows.map(old=>old.id===id?row:old);
  byId("entry").close();write("Transaction saved.");
};
byId("remove").onclick=()=>{const row=data.rows.find(r=>r.id===editId);if(!row)return;byId("entry").close();byId("delete-detail").textContent=row.date+" · "+row.category+" · "+money(row.amount);byId("confirm-delete").showModal();};
byId("delete-form").onsubmit=e=>{e.preventDefault();data.rows=data.rows.filter(r=>r.id!==editId);byId("confirm-delete").close();write("Transaction deleted.");};
byId("limit-form").onsubmit=e=>{e.preventDefault();const value=validAmount(byId("limit-amount"));if(value===null)return;data.budgets[selectedMonth]=value;byId("limit").close();write("Budget saved.");};
byId("status").textContent=storageMessage;render();
