const {test,before}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path');
const root=process.env.STUDY_WEBSITE || 'C:/Users/minii/Desktop/Folder/Website/pfkfks-main';
const directory=path.join(root,'public-flowlog/statistics');
const url=s=>'data:text/javascript;base64,'+Buffer.from(s).toString('base64');
let m,createStore;const db=new Map();let owner='u';
const ap='users/u/flowlog/data/activitySessions/a';
const snap=p=>({id:p.split('/').at(-1),exists:()=>db.has(p),data:()=>structuredClone(db.get(p))});
const sdk={doc:(_, ...p)=>p.join('/'),collection:(_, ...p)=>p.join('/'),where:(f,op,v)=>({f,v}),query:(p,w)=>({p,w}),
getDoc:async p=>snap(p),getDocs:async q=>{const p=q.p||q,w=q.w;return {docs:[...db.keys()].filter(k=>k.startsWith(p+'/')&&!k.slice(p.length+1).includes('/')&&(!w||db.get(k)[w.f]===w.v)).map(snap)}},
runTransaction:async(_,fn)=>{const staged=[];let writes=false;const result=await fn({get:async p=>{assert.equal(writes,false,'all reads precede writes');return snap(p)},set:(p,v)=>{writes=true;staged.push([p,structuredClone(v)])}});staged.forEach(([p,v])=>db.set(p,v));return result},setDoc:async(p,v)=>db.set(p,v)};
before(async()=>{const modelUrl=url(fs.readFileSync(path.join(directory,'study-link-model.js'),'utf8'));m=await import(modelUrl);globalThis.__studySdk=sdk;let s=fs.readFileSync(path.join(directory,'study-link-store.js'),'utf8');s=s.replace(/import\s*\{([\s\S]*?)\}\s*from 'https:[^']+';/,(_,names)=>'const {'+names+'} = globalThis.__studySdk;');s=s.replace("'./study-link-model.js'",JSON.stringify(modelUrl));createStore=(await import(url(s))).createStudyLinkStore;});
const activity=()=>({id:777,startTime:1000,endTime:3000,durationMillis:2000,category:'ETC',title:'activity',revision:0,sourceType:'MANUAL'});
const link=()=>({id:m.makeLinkId('a','c_2026-09-28','STUDY'),activityId:'a',courseId:'c',lessonId:'c_2026-09-28',lessonDate:'2026-09-28',phase:'STUDY',segments:[{startTime:1000,endTime:2000}],snapshot:{courseName:'Math',note:'original',lessonDate:'2026-09-28',time:'09:00',endTime:'10:00',done:['','',''],skipped:false},sourceVersion:'v1',deletedAt:null});
function reset(){db.clear();owner='u';db.set(ap,activity());return createStore({},()=>owner)}
test('browser-produced default RECORD segment saves through actual store with milliseconds', async()=>{
 const fixture=path.resolve(__dirname,'../../app/build/study-validation/panel-record.json');
 if(!fs.existsSync(fixture)) throw Error('Run browser-test.cjs before model-store.test.cjs');
 const input=JSON.parse(fs.readFileSync(fixture,'utf8'));
 const store=reset();db.set(ap,{...activity(),startTime:input.patch.start,endTime:input.patch.end,durationMillis:input.patch.end-input.patch.start});
 await store.load('a');await store.save(input);
 const saved=db.get('users/u/activityStudyLinks/'+input.links[0].id);
 assert.equal(saved.phase,'RECORD');assert.equal(saved.segments[0].startTime,input.patch.start);
 assert.equal(saved.segments[0].endTime,input.patch.end);assert.equal(saved.segments[0].startTime%1000,123);
});
test('whole record link survives source review changes with stable identity and original snapshot',async()=>{
 const store=reset();await store.load('a');
 const l={...link(),phase:'RECORD',id:m.makeLinkId('a','c_2026-09-28','RECORD')};
 await store.save({ownerUid:'u',activityId:'a',baseRevision:0,links:[l],mutationId:'whole'});
 const courses=[{id:'c',name:'New title',start:'2026-09-28',end:'2026-09-28',days:[1]}];
 const records=[{id:l.lessonId,note:'Updated note',done:['2026-09-28','2026-09-29','']}];
 const loaded=await store.load('a');
 const c=m.buildCandidates({courses,records,activity:loaded.activity,existingLinks:loaded.links}).candidates.find(x=>x.id===l.id);
 assert.equal(c.phase,'RECORD');assert.equal(c.currentSource.note,'Updated note');
 assert.deepEqual(c.currentSource.done,records[0].done);assert.equal(c.snapshot.note,'original');
 assert.equal(c.isSnapshotFallback,false);
 const missing=m.buildCandidates({activity:loaded.activity,existingLinks:loaded.links}).candidates.find(x=>x.id===l.id);
 assert.equal(missing.isSnapshotFallback,true);assert.equal(missing.snapshot.note,'original');
});
test('ID distinguishes delimiters and unicode and phase',()=>{assert.notEqual(m.makeLinkId('a~b','c','STUDY'),m.makeLinkId('a','b~c','STUDY'));assert.equal(m.parseLinkId(m.makeLinkId('한글','수학','REVIEW_1')).lessonId,'수학');assert.notEqual(m.makeLinkId('a','c','STUDY'),m.makeLinkId('a','c','REVIEW_1'))});
test('invalid numbers, ranges, phase and overlapping allocations reject',()=>{for(const segments of [[{startTime:NaN,endTime:2000}],[{startTime:0,endTime:2000}],[{startTime:2000,endTime:1000}],[{startTime:1000,endTime:2500},{startTime:2000,endTime:3000}]])assert.throws(()=>m.validateLinks([{...link(),segments}],activity()));assert.throws(()=>m.validateLinks([{...link(),phase:'UNKNOWN'}],activity()));assert.throws(()=>m.validateLinks([link(),{...link(),lessonId:'d',segments:[{startTime:1500,endTime:3000}]}],activity()))});
test('MOVE blocks class but explicit study remains separate',()=>{m.validateLinks([link()],{...activity(),category:'MOVE'});assert.throws(()=>m.validateLinks([{...link(),phase:'COURSE_SESSION'}],{...activity(),category:'MOVE'}))});
test('source lesson date and review due dates remain distinct',()=>{const course={id:'c',name:'Math',start:'2026-09-01',end:'2026-09-30',days:[1],time:'09:00',endTime:'10:00'};assert.equal(m.lessons(course).length,4);const record={done:['2026-09-21','',''],note:'whole source',skipped:false};assert.equal(m.nextTask(record,'2026-09-14').due,'2026-09-22');const snapshot=m.createSnapshot(course,{date:'2026-09-14'},record);assert.equal(snapshot.note,'whole source');assert.equal(snapshot.lessonDate,'2026-09-14')});
test('load canonical document ID, title-only edit does not imply consent',async()=>{const store=reset();const data=await store.load('a');assert.equal(data.activity.id,'a');await store.save({ownerUid:'u',activityId:'a',baseRevision:0,patch:{title:'changed',end:4000},links:[],mutationId:'details'});assert.equal(db.get(ap).durationMillis,3000);assert.equal([...db.keys()].filter(k=>k.includes('interactionDecisions')).length,0)});
test('classification and link save atomically, retry idempotent',async()=>{const store=reset();await store.load('a');const input={ownerUid:'u',activityId:'a',baseRevision:0,patch:{category:'SCHOOL'},links:[link()],mutationId:'m'};await store.save(input);await store.save(input);assert.equal(db.get(ap).revision,1);assert.equal(db.get(ap).originalCategory,'ETC');assert.equal([...db.keys()].filter(k=>k.includes('interactionDecisions')).length,2);assert.equal(db.get('users/u/interactionDecisions/m~CLASSIFICATION_CONFIRM').payload.fromCategory,'ETC')});
test('stale revision and account switch preserve remote data',async()=>{const store=reset();await store.load('a');db.get(ap).revision=2;await assert.rejects(store.save({ownerUid:'u',activityId:'a',baseRevision:0,links:[],mutationId:'m'}));owner='other';await assert.rejects(store.save({ownerUid:'u',activityId:'a',baseRevision:0,links:[],mutationId:'m'}));assert.equal(db.get(ap).revision,2)});
test('snapshot and confirmedAt retained, unlink tombstone logged',async()=>{const store=reset();await store.load('a');const l=link();await store.save({ownerUid:'u',activityId:'a',baseRevision:0,links:[l],mutationId:'m'});const original=db.get('users/u/activityStudyLinks/'+l.id);await store.load('a');await store.save({ownerUid:'u',activityId:'a',baseRevision:1,links:[{...l,snapshot:{note:'new'},deletedAt:4000}],mutationId:'n'});const current=db.get('users/u/activityStudyLinks/'+l.id);assert.deepEqual(current.snapshot,original.snapshot);assert.equal(current.confirmedAt,original.confirmedAt);assert.equal(current.deletedAt,4000);assert.ok(db.has('users/u/interactionDecisions/n~STUDY_LINK_REMOVE'))});
test('missing previously loaded link cannot bypass overlap checks',async()=>{const store=reset();db.set('users/u/activityStudyLinks/'+link().id,{...link(),revision:1});await store.load('a');await assert.rejects(store.save({ownerUid:'u',activityId:'a',baseRevision:0,links:[],mutationId:'x'}))});
test('immutable decision retry does not create second event',async()=>{const store=reset();const d={ownerUid:'u',activityId:'a',proposalId:'p',kind:'RECOMMENDATION_SNOOZE',outcome:'SNOOZED',category:'SCHOOL',activityStartMs:1000};await store.recordDecision(d);const before=structuredClone(db);await store.recordDecision(d);assert.deepEqual(db,before)});
test('paused/ended timer duration is preserved by title edit and limits allocations',async()=>{const store=reset();db.get(ap).durationMillis=500;await store.load('a');await store.save({ownerUid:'u',activityId:'a',baseRevision:0,patch:{title:'paused'},links:[],mutationId:'p'});assert.equal(db.get(ap).durationMillis,500);await store.load('a');await assert.rejects(store.save({ownerUid:'u',activityId:'a',baseRevision:1,links:[link()],mutationId:'q'}));assert.equal(db.get(ap).revision,1)});
