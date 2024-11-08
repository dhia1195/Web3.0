import { NavItem } from './nav-item/nav-item';

export const navItems: NavItem[] = [
  {
    navCap: 'Home',
  },
  {
    displayName: 'Dashboard',
    iconName: 'layout-dashboard',
    route: '/dashboard',
  },
  {
    displayName: 'Personne',
    iconName: 'layout-dashboard',
    route: '/dashboard/personne',
  },
  {
    navCap: 'Ui Components',
  },
  {
    displayName: 'Plateforme',
    iconName: 'globe',
    route: '/ui-components/plateforme',
  },
  {
    displayName: 'Cours',
    iconName: 'school',
    route: '/ui-components/cours',
  },
  {
    displayName: 'evaluation',
    iconName: 'school',
    route: '/ui-components/evaluation',
  },
  {
    displayName: 'activite-educative',
    iconName: 'school',
    route: '/ui-components/activite-educative',},
    {
    displayName: 'Methodes',
    iconName: 'list',
    route: '/ui-components/methode',
  },
  {
    displayName: 'Technologies d` Education',
    iconName: 'school',
    route: '/ui-components/technologie',
  },
  {
    displayName: 'Badge',
    iconName: 'rosette',
    route: '/ui-components/badge',
  },
  {
    displayName: 'Chips',
    iconName: 'poker-chip',
    route: '/ui-components/chips',
  },
  {
    displayName: 'Lists',
    iconName: 'list',
    route: '/ui-components/lists',
  },
  {
    displayName: 'Menu',
    iconName: 'layout-navbar-expand',
    route: '/ui-components/menu',
  },
  {
    displayName: 'Tooltips',
    iconName: 'tooltip',
    route: '/ui-components/tooltips',
  },
  {
    navCap: 'Auth',
  },
  {
    displayName: 'Login',
    iconName: 'lock',
    route: '/authentication/login',
  },
  {
    displayName: 'Register',
    iconName: 'user-plus',
    route: '/authentication/register',
  },
  {
    navCap: 'Extra',
  },
  {
    displayName: 'Icons',
    iconName: 'mood-smile',
    route: '/extra/icons',
  },
  {
    displayName: 'Sample Page',
    iconName: 'aperture',
    route: '/extra/sample-page',
  },
];
