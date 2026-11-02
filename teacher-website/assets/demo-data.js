// Explicitly fictional data for a browser-only teacher dashboard prototype.
// Nothing in this module proves BLE presence or writes to Firebase.
window.SmartAttendDemo = Object.freeze({
  students: [
    {id:'DEMO-001',name:'Aman Sharma',batch:'TE-A',department:'EXTC',attended:22,total:26},
    {id:'DEMO-002',name:'Nisha Rao',batch:'TE-A',department:'EXTC',attended:17,total:26},
    {id:'DEMO-003',name:'Rohit Mehta',batch:'TE-A',department:'EXTC',attended:13,total:26},
    {id:'DEMO-004',name:'Sara Khan',batch:'TE-B',department:'EXTC',attended:24,total:26},
    {id:'DEMO-005',name:'Anjali Patil',batch:'TE-B',department:'EXTC',attended:19,total:26},
    {id:'DEMO-006',name:'Vivek Shah',batch:'TE-B',department:'EXTC',attended:10,total:26}
  ],
  timetable: [
    {id:'SC-DEMO-1',subject:'Database Management Systems',code:'DBMS',batch:'TE-A',room:'302',start:'10:00',end:'11:00',type:'Lecture',day:'Monday'},
    {id:'SC-DEMO-2',subject:'Computer Networks',code:'CN',batch:'TE-A',room:'204',start:'11:15',end:'12:15',type:'Lecture',day:'Monday'},
    {id:'SC-DEMO-3',subject:'Microcontrollers Lab',code:'MC-LAB',batch:'TE-B',room:'Lab 4',start:'13:00',end:'15:00',type:'Lab',day:'Monday'},
    {id:'SC-DEMO-4',subject:'Database Management Systems',code:'DBMS',batch:'TE-B',room:'302',start:'10:00',end:'11:00',type:'Lecture',day:'Tuesday'},
    {id:'SC-DEMO-5',subject:'Computer Networks Lab',code:'CN-LAB',batch:'TE-A',room:'Lab 2',start:'11:30',end:'13:30',type:'Lab',day:'Wednesday'},
    {id:'SC-DEMO-6',subject:'Database Management Systems',code:'DBMS',batch:'TE-A',room:'302',start:'12:30',end:'13:30',type:'Lecture',day:'Thursday'},
    {id:'SC-DEMO-7',subject:'Data Structures',code:'DS',batch:'TE-B',room:'205',start:'10:00',end:'11:00',type:'Lecture',day:'Friday'}
  ],
  corrections: [
    {id:'COR-DEMO-1',student:'Aman Sharma',roll:'DEMO-001',subject:'DBMS',date:'2026-10-10',current:'Absent',expected:'Present',reason:'BLE signal was not detected',status:'Pending'},
    {id:'COR-DEMO-2',student:'Nisha Rao',roll:'DEMO-002',subject:'CN',date:'2026-10-09',current:'Late',expected:'Present',reason:'Entry time seems incorrect',status:'Pending'},
    {id:'COR-DEMO-3',student:'Vivek Shah',roll:'DEMO-006',subject:'DBMS',date:'2026-10-08',current:'Absent',expected:'Review',reason:'Exited after system error',status:'Pending'}
  ],
  notices: [
    {id:'NOTICE-DEMO-1',title:'DBMS Assignment 1',subject:'DBMS',batch:'TE-A',due:'2026-11-05',message:'Submit the relational schema assignment.',created:'2026-10-31'},
    {id:'NOTICE-DEMO-2',title:'Lab notebook review',subject:'MC-LAB',batch:'TE-B',due:'2026-11-08',message:'Bring your practical notebook.',created:'2026-10-31'}
  ]
});
