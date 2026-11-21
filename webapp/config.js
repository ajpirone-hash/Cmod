// Settings for the Gang Box web app.
window.GANGBOX_CONFIG = {
  // Company name shown at the top of the page and in the email subject.
  company: "Action Electrical & Mechanical",
  // Where "Send" emails the list (straight to this inbox, no mail app needed).
  // Uses the free FormSubmit.co service. The first list sent triggers a one-time
  // "confirm" email to this address. After confirming, you can swap the address
  // for the random code FormSubmit gives you, to keep the address private.
  // Leave blank to use the phone's share menu instead.
  sendTo: "apirone@actioncontractors.com",
  // Warehouse email address (only used when sendTo is blank). When set, the "Email" option fills it in.
  // You can also add ?to=name@company.com to the link you send the crews.
  warehouseEmail: "",
  // Action Project PRO: links made from an Awarded job (?t=...) also send the list into that job.
  // These are public web-app values (the same ones the Action Project PRO page uses).
  jobsApi: { projectId: "action-project-pro-d38d9", apiKey: "AIzaSyCGvTCBJVSUc-V6655Ukbqjomnv6d_a9t0" },
};
