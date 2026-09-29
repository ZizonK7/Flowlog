const assert=require('node:assert/strict');
const base='http://127.0.0.1:8187/v1/projects/demo-flowlog-study/databases/(default)/documents';
const token=uid=>['eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0',Buffer.from(JSON.stringify({sub:uid,user_id:uid,aud:'demo-flowlog-study',iss:'https://securetoken.google.com/demo-flowlog-study',iat:Math.floor(Date.now()/1000),exp:Math.floor(Date.now()/1000)+3600,firebase:{sign_in_provider:'custom'}})).toString('base64url'),''].join('.');
const encode=v=>v===null?{nullValue:null}:typeof v==='string'?{stringValue:v}:typeof v==='boolean'?{booleanValue:v}:typeof v==='number'?{integerValue:String(v)}:Array.isArray(v)?{arrayValue:{values:v.map(encode)}}:{mapValue:{fields:Object.fromEntries(Object.entries(v).map(([k,x])=>[k,encode(x)]))}};
async function req(uid,path,data){const r=await fetch(base+'/'+path,{method:data?'PATCH':'GET',headers:{Authorization:'Bearer '+(uid==='adminSeed'?'owner':token(uid)),'Content-Type':'application/json'},body:data?JSON.stringify({fields:encode(data).mapValue.fields}):undefined});return {status:r.status,text:await r.text()}}
async function expect(uid,path,data,status){const r=await req(uid,path,data);assert.equal(r.status,status,r.text);}
(async()=>{
const path='users/u/flowlog/data/activitySessions/a';
const activity={revision:1,startTime:100,endTime:200,durationMillis:100,title:'class',category:'SCHOOL',modifiedTime:200};
await expect('u',path,activity,200);
await expect('u',path,{...activity,title:'old writer'},403);
await expect('u',path,{...activity,revision:2},200);
await expect('other',path,undefined,403);
const link={schemaVersion:1,activityId:'a',courseId:'c',lessonId:'c_2026-09-28',lessonDate:'2026-09-28',phase:'COURSE_SESSION',segments:[{startTime:110,endTime:190}],snapshot:{courseName:'C'},sourceVersion:'hash',confirmedAt:200,updatedAt:200,revision:1,deletedAt:null};
await expect('u','users/u/activityStudyLinks/a~c~COURSE_SESSION',link,200);
await expect('u','users/u/activityStudyLinks/a~c~RECORD',{...link,phase:'RECORD'},200);
await expect('other','users/u/activityStudyLinks/a~c~RECORD',undefined,403);
await expect('other','users/u/activityStudyLinks/a~c~COURSE_SESSION',undefined,403);
await expect('xrUHsuQ8l2WJtB6gt0Ynp0U5VkJ3','users/u/activityStudyLinks/a~c~COURSE_SESSION',undefined,403);
await expect('u','users/u/activityStudyLinks/bad',{...link,segments:[{startTime:50,endTime:190}]},403);
await expect('u','users/u/activityStudyLinks/orphan',{...link,activityId:'missing',deletedAt:300},200);
await expect('u','users/u/flowlog/config',{mainButton:{buttons:Array(11).fill({category:'ETC'})}},403);
await expect('u','users/u/flowlog/config',{mainButton:{buttons:Array(10).fill({category:'ETC'})}},200);
const d={schemaVersion:1,decisionId:'m~CLASSIFICATION_CONFIRM',proposalId:'m',kind:'CLASSIFICATION_CONFIRM',activityId:'a',category:'SCHOOL',localDate:'2026-09-28',minuteOfDay:540,weekday:1,outcome:'APPLIED',createdAt:200,revision:1,payload:{}};
await expect('u','users/u/interactionDecisions/'+d.decisionId,d,200);
await expect('u','users/u/interactionDecisions/'+d.decisionId,{...d,category:'MOVE'},403);
console.log('15 Firestore authorization/revision assertions passed');
})().catch(e=>{console.error(e);process.exitCode=1});
