const assert=require('node:assert/strict');
const adminUid=process.env.FLOWLOG_ADMIN_UID||'test-admin-uid'; // same default as prepare-rules.cjs
const base='http://127.0.0.1:8187/v1/projects/demo-flowlog-study/databases/(default)/documents';
const token=uid=>['eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0',Buffer.from(JSON.stringify({sub:uid,user_id:uid,aud:'demo-flowlog-study',iss:'https://securetoken.google.com/demo-flowlog-study',iat:Math.floor(Date.now()/1000),exp:Math.floor(Date.now()/1000)+3600,firebase:{sign_in_provider:'custom'}})).toString('base64url'),''].join('.');
const encode=v=>v===null?{nullValue:null}:typeof v==='string'?{stringValue:v}:typeof v==='boolean'?{booleanValue:v}:typeof v==='number'?{integerValue:String(v)}:Array.isArray(v)?{arrayValue:{values:v.map(encode)}}:{mapValue:{fields:Object.fromEntries(Object.entries(v).map(([k,x])=>[k,encode(x)]))}};
async function req(uid,path,data){const r=await fetch(base+'/'+path,{method:data?'PATCH':'GET',headers:{Authorization:'Bearer '+(uid==='adminSeed'?'owner':token(uid)),'Content-Type':'application/json'},body:data?JSON.stringify({fields:encode(data).mapValue.fields}):undefined});return {status:r.status,text:await r.text()}}
async function expect(uid,path,data,status){const r=await req(uid,path,data);assert.equal(r.status,status,r.text);}
// Write with server-time fields (request.time) through :commit, like FieldValue.serverTimestamp().
async function expectCommit(uid,path,data,serverTimeFields,status){const name='projects/demo-flowlog-study/databases/(default)/documents/'+path;const write={update:{name,fields:encode(data).mapValue.fields}};if(serverTimeFields.length)write.updateTransforms=serverTimeFields.map(f=>({fieldPath:f,setToServerValue:'REQUEST_TIME'}));const r=await fetch(base.replace(/\/documents$/,'/documents:commit'),{method:'POST',headers:{Authorization:'Bearer '+token(uid),'Content-Type':'application/json'},body:JSON.stringify({writes:[write]})});assert.equal(r.status,status,await r.text());}
(async()=>{
const path='users/u/flowlog/data/activitySessions/a';
const activity={revision:1,startTime:100,endTime:200,durationMillis:100,title:'class',category:'SCHOOL',modifiedTime:200};
await expect('u',path,activity,200);
await expect('u',path,{...activity,title:'old writer'},403);
await expect('u',path,{...activity,revision:2},200);
await expect('other',path,undefined,403);
// Content-free tombstone (deleted record, wave 2) still satisfies validActivity.
await expect('u','users/u/flowlog/data/activitySessions/t',{...activity,title:'',note:null,tags:[],exerciseSets:[],isDeleted:true,deletedAt:300},200);
const link={schemaVersion:1,activityId:'a',courseId:'c',lessonId:'c_2026-09-28',lessonDate:'2026-09-28',phase:'COURSE_SESSION',segments:[{startTime:110,endTime:190}],snapshot:{courseName:'C'},sourceVersion:'hash',confirmedAt:200,updatedAt:200,revision:1,deletedAt:null};
await expect('u','users/u/activityStudyLinks/a~c~COURSE_SESSION',link,200);
await expect('u','users/u/activityStudyLinks/a~c~RECORD',{...link,phase:'RECORD'},200);
await expect('other','users/u/activityStudyLinks/a~c~RECORD',undefined,403);
await expect('other','users/u/activityStudyLinks/a~c~COURSE_SESSION',undefined,403);
await expect(adminUid,'users/u/activityStudyLinks/a~c~COURSE_SESSION',undefined,403);
await expect('u','users/u/activityStudyLinks/bad',{...link,segments:[{startTime:50,endTime:190}]},403);
await expect('u','users/u/activityStudyLinks/orphan',{...link,activityId:'missing',deletedAt:300},200);
await expect('u','users/u/flowlog/config',{mainButton:{buttons:Array(11).fill({category:'ETC'})}},403);
await expect('u','users/u/flowlog/config',{mainButton:{buttons:Array(10).fill({category:'ETC'})}},200);
const d={schemaVersion:1,decisionId:'m~CLASSIFICATION_CONFIRM',proposalId:'m',kind:'CLASSIFICATION_CONFIRM',activityId:'a',category:'SCHOOL',localDate:'2026-09-28',minuteOfDay:540,weekday:1,outcome:'APPLIED',createdAt:200,revision:1,payload:{}};
await expect('u','users/u/interactionDecisions/'+d.decisionId,d,200);
await expect('u','users/u/interactionDecisions/'+d.decisionId,{...d,category:'MOVE'},403);
// Survey responses: signed in only, document id = uid, fixed fields, bounded text.
const survey={userId:'u',answers:['좋음','보통','조금 졸림'],note:'메모'};
let anon=await fetch(base+'/flowlogSurveyResponses/u',{method:'PATCH',headers:{'Content-Type':'application/json'},body:JSON.stringify({fields:encode({...survey,createdAt:1}).mapValue.fields})});
assert.equal(anon.status,403,'signed-out survey write must be rejected');
await expectCommit('u','flowlogSurveyResponses/u',survey,['createdAt'],200);
await expectCommit('u','flowlogSurveyResponses/u',survey,['createdAt'],403); // second response (update) rejected
await expectCommit('v','flowlogSurveyResponses/other',{...survey,userId:'v'},['createdAt'],403); // id must be own uid
await expectCommit('w','flowlogSurveyResponses/w',{...survey,userId:'w',answers:['x'.repeat(2001)]},['createdAt'],403);
await expectCommit('w','flowlogSurveyResponses/w',{...survey,userId:'w',note:'x'.repeat(2001)},['createdAt'],403);
await expectCommit('w','flowlogSurveyResponses/w',{...survey,userId:'w',extra:'field'},['createdAt'],403);
await expectCommit('w','flowlogSurveyResponses/w',{...survey,userId:'w'},['createdAt'],200);
// Account deletion reservation: only its two fields, server time, owner only.
await expectCommit('u','users/u/flowlog/account',{deletionRequestedBy:'app'},['deletionRequestedAt'],200);
await expectCommit('u','users/u/flowlog/account',{},[],200); // cancel = both fields removed
await expectCommit('u','users/u/flowlog/account',{deletionRequestedBy:'app',note:'x'},['deletionRequestedAt'],403);
await expectCommit('u','users/u/flowlog/account',{deletionRequestedBy:'app',deletionRequestedAt:5},[],403); // client clock
await expectCommit('other','users/u/flowlog/account',{deletionRequestedBy:'app'},['deletionRequestedAt'],403);
// Size cap on generic flowlog documents and records.
const wide=Object.fromEntries(Array.from({length:101},(_,i)=>['f'+i,i]));
await expect('u','users/u/flowlog/metadata',{updatedAt:1},200);
await expect('u','users/u/flowlog/metadata',wide,403);
await expect('u','users/u/flowlog/data/eventLogs/e1',{eventId:'e1',eventType:'X'},200);
await expect('u','users/u/flowlog/data/eventLogs/e2',wide,403);
// Shared running timer (users/{uid}/flowlog/timer): owner only, fixed fields, server time.
const timer={status:'running',category:'STUDY',title:'t',startTime:100,pausedElapsedMs:0,linkedTodoId:null,updatedBy:'app',deviceId:'d1'};
await expectCommit('u','users/u/flowlog/timer',timer,['updatedAt'],200);
await expectCommit('u','users/u/flowlog/timer',{...timer,status:'stopped',updatedBy:'web'},['updatedAt'],200);
await expectCommit('u','users/u/flowlog/timer',{...timer,note:'x'},['updatedAt'],403); // unknown field
await expectCommit('u','users/u/flowlog/timer',{...timer,status:'done'},['updatedAt'],403); // unknown status
await expectCommit('u','users/u/flowlog/timer',{...timer,updatedAt:5},[],403); // client clock instead of server time
await expectCommit('other','users/u/flowlog/timer',timer,['updatedAt'],403); // someone else's timer
await expect('u','users/u/flowlog/timer',undefined,200);
await expect('other','users/u/flowlog/timer',undefined,403);
await expect(adminUid,'users/u/flowlog/timer',undefined,403); // owner only, no admin read
await expect(adminUid,'users/u/flowlog/config',undefined,200); // admin read of other flowlog documents unchanged
console.log('43 Firestore authorization/revision assertions passed');
})().catch(e=>{console.error(e);process.exitCode=1});
