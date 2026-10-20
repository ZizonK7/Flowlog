const fs=require('node:fs'),path=require('node:path');
const root=path.resolve(__dirname,'../..'),stage=path.join(root,'app/build/study-validation');
fs.mkdirSync(stage,{recursive:true});
// The repository copy has placeholders instead of account ids; fill them for the emulator.
// Real ids are never stored here: set FLOWLOG_ADMIN_UID / FLOWLOG_WORK_STAFF_UID only if needed.
const ids={__FLOWLOG_ADMIN_UID__:process.env.FLOWLOG_ADMIN_UID||'test-admin-uid',__FLOWLOG_WORK_STAFF_UID__:process.env.FLOWLOG_WORK_STAFF_UID||'test-work-staff-uid'};
const rules=fs.readFileSync(path.join(root,'docs/study-integration/firestore.proposed.rules'),'utf8').replace(/__FLOWLOG_(ADMIN|WORK_STAFF)_UID__/g,m=>ids[m]);
fs.writeFileSync(path.join(stage,'firestore.rules'),rules);
fs.copyFileSync(path.join(__dirname,'rules-test.cjs'),path.join(stage,'rules-test.cjs'));
fs.writeFileSync(path.join(stage,'firebase.json'),JSON.stringify({firestore:{rules:'firestore.rules'},emulators:{firestore:{port:8187},ui:{enabled:false},singleProjectMode:true}}));
console.log(stage);
