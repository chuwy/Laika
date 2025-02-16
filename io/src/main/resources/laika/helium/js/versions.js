
function logError (req) {
  const status = req.status;
  const text = req.statusText;
  console.log(`[${status}]: ${text}`);
}

function populateMenu (data, localRootPrefix, currentPath, currentVersion, siteBaseURL) {
  const currentTarget = data.linkTargets.find(target => target.path === currentPath);
  const menuList = document.getElementById("version-list");
  let canonicalLink;
  data.versions.forEach(version => {
    const pathPrefix = localRootPrefix + version.pathSegment;
    const hasMatchingLink = currentTarget && currentTarget.versions.includes(version.pathSegment);
    const href = (hasMatchingLink) ? pathPrefix + currentPath : pathPrefix + version.fallbackLink;
    if (version.canonical && hasMatchingLink) {
      const versionedPath = version.pathSegment + currentPath
      canonicalLink = (siteBaseURL != null) ? siteBaseURL + versionedPath : localRootPrefix + versionedPath;
    }

    const link = document.createElement('a');
    if (version.label) {
      const span = document.createElement("span");
      span.innerText = version.label;
      span.classList.add(version.label.toLowerCase());
      span.classList.add("version-label");
      const wrapper = document.createElement("span");
      wrapper.classList.add("left-column");
      wrapper.appendChild(span);
      link.appendChild(wrapper);
    }
    link.appendChild(document.createTextNode(version.displayValue));
    link.setAttribute("href", href);
    
    const listItem = document.createElement("li");
    listItem.classList.add("level1");
    if (version.pathSegment === currentVersion) listItem.classList.add("active");
    listItem.appendChild(link);

    menuList.appendChild(listItem);
  });
  return canonicalLink;
}

function loadVersions (localRootPrefix, currentPath, currentVersion, siteBaseURL) {
  const url = localRootPrefix + "laika/versionInfo.json";
  const req = new XMLHttpRequest();
  req.open("GET", url);
  req.responseType = "json";
  req.onload = () => {
    if (req.status === 200) {
      const canonicalLink = populateMenu(req.response, localRootPrefix, currentPath, currentVersion, siteBaseURL);
      initMenuToggle();
      if (canonicalLink) insertCanonicalLink(canonicalLink);
    }
    else logError(req)
  };
  req.onerror = () => {
    logError(req)
  };
  req.send();
}

function initMenuToggle () {
  document.addEventListener("click", (evt) => {
    const menuClicked = evt.target.closest("#version-menu");
    const buttonClicked = evt.target.closest("#version-menu-toggle");
    if (!menuClicked && !buttonClicked) {
      document.getElementById("version-menu").classList.remove("versions-open")
    }
  });
  document.getElementById("version-menu-toggle").onclick = () => {
    document.getElementById("version-menu").classList.toggle("versions-open");
  };
}

function insertCanonicalLink (linkHref) {
  if (!document.querySelector("link[rel='canonical']")) {
    const head = document.head;
    const link = document.createElement("link");
    link.setAttribute("rel", "canonical");
    link.setAttribute("href", linkHref);
    head.appendChild(link);
  }
}

function initVersions (localRootPrefix, currentPath, currentVersion, siteBaseURL) {
  document.addEventListener('DOMContentLoaded', () => {
    loadVersions(localRootPrefix, currentPath, currentVersion, siteBaseURL);
  });
}
