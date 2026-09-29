const fs=require('node:fs'),path=require('node:path'),cp=require('node:child_process'),crypto=require('node:crypto');
const roots=[path.resolve(__dirname,'../..'),process.env.STUDY_WEBSITE||'C:/Users/minii/Desktop/Folder/Website/pfkfks-main'];
const sections=[];
for(const root of roots){
 const git=(args)=>cp.execFileSync('git',['-C',root,...args],{encoding:'utf8',maxBuffer:5e6});
 sections.push('REPOSITORY '+root+'\n'+git(['diff','--no-ext-diff']));
 const untracked=git(['ls-files','--others','--exclude-standard']).trim().split('\n').filter(p=>p&&!p.startsWith('docs/STUDY_FLOWLOG_')&&!p.endsWith('firestore.original.rules'));
 for(const p of untracked){sections.push('NEW FILE '+p+'\n'+fs.readFileSync(path.join(root,p),'utf8'));}
 const tracked=git(['diff','--name-only']).trim().split('\n').filter(Boolean);
 sections.push('FILE SHA256 MANIFEST\n'+[...tracked,...untracked].sort().map(p=>crypto.createHash('sha256').update(fs.readFileSync(path.join(root,p))).digest('hex')+' '+p).join('\n'));
}
fs.mkdirSync(path.join(roots[0],'app/build/study-validation'),{recursive:true});
fs.writeFileSync(path.join(roots[0],'app/build/study-validation/review-artifact.txt'),sections.join('\n\n'));
console.log('Review bundle bytes:',Buffer.byteLength(sections.join('\n\n')));
