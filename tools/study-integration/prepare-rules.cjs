const fs=require('node:fs'),path=require('node:path');
const root=path.resolve(__dirname,'../..'),stage=path.join(root,'app/build/study-validation');
fs.mkdirSync(stage,{recursive:true});
fs.copyFileSync(path.join(root,'docs/study-integration/firestore.proposed.rules'),path.join(stage,'firestore.rules'));
fs.copyFileSync(path.join(__dirname,'rules-test.cjs'),path.join(stage,'rules-test.cjs'));
fs.writeFileSync(path.join(stage,'firebase.json'),JSON.stringify({firestore:{rules:'firestore.rules'},emulators:{firestore:{port:8187},ui:{enabled:false},singleProjectMode:true}}));
console.log(stage);
