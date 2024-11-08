import { Component } from '@angular/core';

@Component({
  selector: 'app-branding',
  template: `
  <div class="branding">
  <a href="/" class="branding-link">
    <img
      src="./assets/images/logos/Pasted image1.png"
      class="logo-small align-middle m-2"
      alt="logo"
    />
    <span class="branding-title">Infinity</span>
  </a>
</div>
<style>
  /* Make the logo bigger */
  .logo-small {
    width: 100px; /* Adjust the size as needed */
    height: auto; /* Maintain aspect ratio */
    margin-right: 10px; /* Space between the logo and the title */
  }

  /* Styling for the title next to the logo */
  .branding-title {
    font-size: 24px; /* Adjust font size */
    font-weight: bold; /* Make the title bold */
    color: #333; /* Adjust color */
    vertical-align: middle; /* Align vertically with the logo */
  }

  /* Optional: Style the link to remove underlining */
  .branding-link {
    text-decoration: none;
    display: flex;
    align-items: center;
  }
</style>
`
,
})
export class BrandingComponent {
  constructor() {}
}
