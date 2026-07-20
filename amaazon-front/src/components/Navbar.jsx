import { useState } from 'react';
import './Navbar.css';

function Navbar() {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('All');

  const categories = [
    'All', 'Arts & Crafts', 'Automotive', 'Baby', 'Beauty & Personal Care',
    'Books', 'Clothing', 'Computers', 'Electronics', 'Garden',
    'Health', 'Home', 'Kitchen', 'Movies & TV', 'Music',
    'Pet Supplies', 'Sports', 'Tools', 'Toys & Games', 'Video Games',
  ];

  return (
    <nav className="navbar" id="navbar">
      {/* Logo */}
      <a href="/" className="navbar__logo" id="navbar-logo">
        <span className="navbar__logo-text">amaazon</span>
        <span className="navbar__logo-suffix">.clone</span>
      </a>

      {/* Deliver to */}
      <div className="navbar__deliver" id="navbar-deliver">
        <span className="navbar__deliver-label">Deliver to</span>
        <div className="navbar__deliver-location">
          <span className="navbar__deliver-icon">📍</span>
          <span className="navbar__deliver-country">Republic of Korea</span>
        </div>
      </div>

      {/* Search Bar */}
      <div className="navbar__search" id="navbar-search">
        <select
          className="navbar__search-select"
          value={selectedCategory}
          onChange={(e) => setSelectedCategory(e.target.value)}
        >
          {categories.map((cat) => (
            <option key={cat} value={cat}>{cat}</option>
          ))}
        </select>
        <input
          className="navbar__search-input"
          type="text"
          placeholder="Search Amaazon"
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
        />
        <button className="navbar__search-btn" id="navbar-search-btn">
          <svg viewBox="0 0 24 24" width="22" height="22" fill="#131921">
            <path d="M10.5 3a7.5 7.5 0 015.645 12.438l4.709 4.708a.75.75 0 01-1.06 1.06l-4.709-4.708A7.5 7.5 0 1110.5 3zm0 1.5a6 6 0 100 12 6 6 0 000-12z" />
          </svg>
        </button>
      </div>

      {/* Right section */}
      <div className="navbar__right" id="navbar-right">
        {/* Language */}
        <div className="navbar__option navbar__language" id="navbar-lang">
          <span className="navbar__flag">🇺🇸</span>
          <span className="navbar__option-line1">EN</span>
          <span className="navbar__dropdown-arrow">▾</span>
        </div>

        {/* Account */}
        <a href="#" className="navbar__option" id="navbar-account">
          <span className="navbar__option-line1">Hello, sign in</span>
          <span className="navbar__option-line2">Account & Lists <span className="navbar__dropdown-arrow">▾</span></span>
        </a>

        {/* Orders */}
        <a href="#" className="navbar__option" id="navbar-orders">
          <span className="navbar__option-line1">Returns</span>
          <span className="navbar__option-line2">& Orders</span>
        </a>

        {/* Cart */}
        <a href="#" className="navbar__cart" id="navbar-cart">
          <div className="navbar__cart-icon">
            <span className="navbar__cart-count">0</span>
            <svg viewBox="0 0 40 35" width="40" height="35" fill="none" stroke="#fff" strokeWidth="2">
              <path d="M8 10h28l-3 15H11z" fill="none" />
              <circle cx="14" cy="30" r="2.5" fill="#fff" />
              <circle cx="30" cy="30" r="2.5" fill="#fff" />
              <path d="M2 2h4l2 6" />
            </svg>
          </div>
          <span className="navbar__cart-text">Cart</span>
        </a>
      </div>
    </nav>
  );
}

export default Navbar;
